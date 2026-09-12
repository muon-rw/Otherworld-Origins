package dev.muon.raven_dnd_origins;

import com.seniors.justlevelingfork.registry.RegistrySkills;
import com.seniors.justlevelingfork.registry.skills.Skill;
import dev.muon.raven_dnd_origins.effect.BattleHymnEffect;
import dev.muon.raven_dnd_origins.effect.CuttingWordsEffect;
import dev.muon.raven_dnd_origins.effect.ModEffects;
import dev.muon.raven_dnd_origins.power.DeflectProjectilePower;
import dev.muon.raven_dnd_origins.power.DirectionalTeleportPower;
import dev.muon.raven_dnd_origins.power.HealFromDamagePower;
import dev.muon.raven_dnd_origins.power.ModPowers;
import dev.muon.raven_dnd_origins.power.ModifyCriticalHitPower;
import dev.muon.raven_dnd_origins.power.ModifyDamageTakenDirectPower;
import dev.muon.raven_dnd_origins.power.RecastSpellPower;
import dev.muon.raven_dnd_origins.restrictions.EnchantmentRestrictions;
import dev.muon.raven_dnd_origins.restrictions.SpellRestrictions;
import dev.muon.raven_dnd_origins.selection.SelectionSessions;
import dev.muon.raven_dnd_origins.skills.ModSkills;
import dev.muon.raven_dnd_origins.util.EnhancedRepairLogic;
import dev.muon.raven_dnd_origins.util.RepairMaterialDescription;
import dev.muon.raven_dnd_origins.util.spell.RecentSpellCastCache;
import dev.overgrown.apoli.power.PowerLookup;
import io.redspace.ironsspellbooks.api.events.ModifySpellLevelEvent;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.entity.spells.AbstractConeProjectile;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.apache.commons.lang3.mutable.MutableFloat;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = RavenDndOrigins.MODID)
public class RavenDndOriginsEvents {
    private static final int CONE_DURATION_TICKS = 10;
    private static final Map<Integer, Integer> activeCones = new HashMap<>();
    private static int recentSpellCastMaintenanceCounter;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (++recentSpellCastMaintenanceCounter >= 100) {
            recentSpellCastMaintenanceCounter = 0;
            RecentSpellCastCache.maintenancePrune(event.getServer());
        }
        DirectionalTeleportPower.tickPendingMirrorSounds();
        RecastSpellPower.tickPendingRecasts(event.getServer());
        activeCones.entrySet().removeIf(entry -> {
            int age = entry.getValue();
            if (age < CONE_DURATION_TICKS) {
                entry.setValue(age + 1);
                return false;
            }
            for (Level level : event.getServer().getAllLevels()) {
                if (level.getEntity(entry.getKey()) instanceof AbstractConeProjectile cone) {
                    cone.discard();
                    break;
                }
            }
            return true;
        });
    }

    @SubscribeEvent
    public static void onPlayerLoggedOutSpellCache(PlayerEvent.PlayerLoggedOutEvent event) {
        RecentSpellCastCache.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onLivingDeathSpellCache(LivingDeathEvent event) {
        RecentSpellCastCache.remove(event.getEntity().getUUID());
    }

    /**
     * Non-player mobs leave the level on unload/despawn/discarding; players also fire this when
     * changing dimension, so we must not remove player rows here (see {@link PlayerEvent.PlayerLoggedOutEvent}).
     */
    @SubscribeEvent
    public static void onNonPlayerLivingLeaveLevelSpellCache(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide) {
            return;
        }
        if (event.getEntity() instanceof LivingEntity living && !(living instanceof Player)) {
            RecentSpellCastCache.remove(living.getUUID());
        }
    }

    @SubscribeEvent
    public static void onServerStoppingSpellCache(ServerStoppingEvent event) {
        RecentSpellCastCache.clear();
    }

    @SubscribeEvent
    public static void onTagsUpdatedRepairTooltipCache(TagsUpdatedEvent event) {
        RepairMaterialDescription.invalidate();
    }

    @SubscribeEvent
    public static void onMasterworkAttributeModifiers(ItemAttributeModifierEvent event) {
        EnhancedRepairLogic.onItemAttributeModifiers(event);
    }

    @SubscribeEvent
    public static void onSpellPreCast(SpellPreCastEvent event) {
        if (event.getCastSource() == CastSource.COMMAND || event.getCastSource() == CastSource.SCROLL) {
            return;
        }
        if (!SpellRestrictions.isSpellAllowed(event.getEntity(), SpellRegistry.getSpell(event.getSpellId()))) {
            event.setCanceled(true);
            event.getEntity().displayClientMessage(
                    Component.literal("You are not attuned to this type of magic!")
                            .withStyle(ChatFormatting.RED),
                    true
            );
        }
    }

    @SubscribeEvent
    public static void modifySpellLevels(ModifySpellLevelEvent event) {
        if (event.getEntity() instanceof Player player && ModSkills.WISDOM.get().isEnabled(player)) {
            event.addLevels(1);
        }
    }

    public static void trackConeProjectile(AbstractConeProjectile coneProjectile) {
        activeCones.put(coneProjectile.getId(), 0);
    }

    // Reprompting manually with our own checks, server-side only; Origins' own login flow races
    // with per-layer synchronization.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            SelectionSessions.reconcile(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onCriticalHit(CriticalHitEvent event) {
        Player player = event.getEntity();

        if (event.getTarget() instanceof Player targetPlayer
                && PowerLookup.hasActive(targetPlayer, ModPowers.PREVENT_CRITICAL_HIT)) {
            event.setDamageMultiplier(1.0f);
            event.setCriticalHit(false);
            return;
        }

        float totalModifier = 0f;
        for (ModifyCriticalHitPower.Configuration cfg : PowerLookup.active(
                player, ModPowers.MODIFY_CRITICAL_HIT, ModifyCriticalHitPower.Configuration.class)) {
            totalModifier += cfg.amount();
        }
        if (totalModifier != 0) {
            event.setDamageMultiplier(event.getDamageMultiplier() * (1 + totalModifier));
            event.setCriticalHit(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPsionicWarpFallImmunity(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!event.getSource().is(DamageTypes.FALL)) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!DirectionalTeleportPower.hasFallImmunity(player)) {
            return;
        }
        event.setCanceled(true);
        player.resetFallDistance();
    }

    @SubscribeEvent
    public static void onPsionicWarpClearFallImmunity(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!DirectionalTeleportPower.hasFallImmunity(player)) {
            return;
        }
        if (player.onGround()) {
            DirectionalTeleportPower.clearFallImmunity(player);
            player.resetFallDistance();
        }
    }

    /**
     * Cancel the attack entirely when {@link HealFromDamagePower} matches, so hurt sound,
     * red flash, and camera tilt are all suppressed alongside the damage.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void healFromDamage(LivingIncomingDamageEvent event) {
        if (HealFromDamagePower.apply(event.getEntity(), event.getSource(), event.getAmount())) {
            event.setCanceled(true);
        }
    }

    /**
     * Selection invulnerability: a player with an open selection session can't be hurt while
     * choosing. Origins itself never makes a selecting player invulnerable, so the session is the
     * only signal.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void selectionInvulnerability(LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player && SelectionSessions.get(player).isPresent()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void modifyDamageTakenDirect(LivingDamageEvent.Pre event) {
        float amount = ModifyDamageTakenDirectPower.modify(event.getEntity(), event.getSource(), event.getNewDamage());
        // Favored Foe: +2% damage per effect level (level 10 = +20%)
        var favoredFoe = event.getEntity().getEffect(ModEffects.FAVORED_FOE);
        if (favoredFoe != null) {
            int level = favoredFoe.getAmplifier() + 1;
            amount *= 1 + 0.02f * level;
        }
        if (event.getEntity() instanceof Player player) {
            Skill diamondSkin = RegistrySkills.DIAMOND_SKIN.get();
            if (diamondSkin != null && diamondSkin.isEnabled(player)) {
                amount *= player.isCrouching() ? 0.7f : 0.85f;
            }
        }
        event.setNewDamage(amount);
    }

    /**
     * Deflect projectiles before impact when the power applies. Cancelling prevents the hit so the
     * projectile continues flying (after we redirect it). Deflecting bypasses the damage pipeline
     * entirely.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getRayTraceResult().getType() != HitResult.Type.ENTITY) return;
        Entity hitEntity = ((EntityHitResult) event.getRayTraceResult()).getEntity();
        if (!(hitEntity instanceof LivingEntity living)) return;
        if (living.level().isClientSide()) return;

        var projectile = event.getProjectile();
        var deflectPower = DeflectProjectilePower.tryDeflect(living, projectile);
        if (deflectPower.isPresent()) {
            event.setCanceled(true);
            DeflectProjectilePower.executeDeflect(deflectPower.get(), living, projectile);
        }
    }

    /**
     * Cutting Words: while the damage dealer has the effect, all damage they deal is reduced (amplifier tier).
     */
    @SubscribeEvent
    public static void onCuttingWordsOutgoingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        LivingEntity dealer = CuttingWordsEffect.resolveOutgoingDamageDealer(event.getSource());
        if (dealer == null) {
            return;
        }
        var cuttingWords = dealer.getEffect(ModEffects.CUTTING_WORDS);
        if (cuttingWords == null) {
            return;
        }
        event.setAmount(event.getAmount() * CuttingWordsEffect.outgoingDamageMultiplier(cuttingWords.getAmplifier()));
    }

    /**
     * Battle Hymn: while the damage dealer has the effect, all damage they deal is increased (amplifier tier).
     */
    @SubscribeEvent
    public static void onBattleHymnOutgoingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        LivingEntity dealer = BattleHymnEffect.resolveOutgoingDamageDealer(event.getSource());
        if (dealer == null) {
            return;
        }
        var battleHymn = dealer.getEffect(ModEffects.BATTLE_HYMN);
        if (battleHymn == null) {
            return;
        }
        event.setAmount(event.getAmount() * BattleHymnEffect.outgoingDamageMultiplier(battleHymn.getAmplifier()));
    }

    /**
     * Strip the melee damage a restricted enchantment contributed; the enchantment itself cannot be
     * kept off the weapon.
     */
    @SubscribeEvent
    public static void onRestrictedEnchantmentDamage(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof Player player)) {
            return;
        }
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }

        ItemStack weapon = player.getMainHandItem();
        LivingEntity victim = event.getEntity();
        float damageReduction = restrictedEnchantmentDamage(level, player, weapon, Enchantments.SHARPNESS, victim, source)
                + restrictedEnchantmentDamage(level, player, weapon, Enchantments.SMITE, victim, source)
                + restrictedEnchantmentDamage(level, player, weapon, Enchantments.BANE_OF_ARTHROPODS, victim, source);

        if (damageReduction > 0) {
            event.setAmount(Math.max(0, event.getAmount() - damageReduction));
        }
    }

    private static float restrictedEnchantmentDamage(ServerLevel level, Player player, ItemStack weapon,
                                                     ResourceKey<Enchantment> key, LivingEntity victim,
                                                     DamageSource source) {
        if (EnchantmentRestrictions.isEnchantmentAllowed(player, weapon, key)) {
            return 0;
        }
        Holder<Enchantment> enchantment = level.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(key);
        int enchantmentLevel = weapon.getEnchantmentLevel(enchantment);
        if (enchantmentLevel <= 0) {
            return 0;
        }
        // Ask the enchantment's own data-driven DAMAGE effect, so datapack and mod overrides
        // (and the sensitive_to_smite / sensitive_to_bane_of_arthropods conditions) are honoured.
        MutableFloat contributed = new MutableFloat(0);
        enchantment.value().modifyDamage(level, enchantmentLevel, weapon, victim, source, contributed);
        return contributed.floatValue();
    }
}
