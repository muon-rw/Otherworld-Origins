package dev.muon.otherworldorigins;

import com.seniors.justlevelingfork.registry.RegistrySkills;
import com.seniors.justlevelingfork.registry.skills.Skill;
import dev.muon.otherworldorigins.network.CloseCurrentScreenMessage;
import dev.muon.otherworldorigins.network.OpenOriginScreenMessage;
import dev.muon.otherworldorigins.power.ModifyCriticalHitPower;
import dev.muon.otherworldorigins.restrictions.EnchantmentRestrictions;
import dev.muon.otherworldorigins.restrictions.SpellRestrictions;
import dev.shadowsoffire.apotheosis.Apotheosis;
import io.github.apace100.apoli.util.modifier.ModifierUtil;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.registry.ModComponents;
import io.redspace.ironsspellbooks.api.events.ModifySpellLevelEvent;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.entity.spells.AbstractConeProjectile;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.extensions.IForgeItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

@Mod.EventBusSubscriber(modid = OtherworldOrigins.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEvents {
    private static final int CONE_DURATION_TICKS = 10;
    private static final Map<Integer, Integer> activeCones = new HashMap<>();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            activeCones.entrySet().removeIf(entry -> {
                int coneId = entry.getKey();
                int age = entry.getValue();
                if (age >= CONE_DURATION_TICKS) {
                    for (Level level : event.getServer().getAllLevels()) {
                        AbstractConeProjectile cone = (AbstractConeProjectile) level.getEntity(coneId);
                        if (cone != null) {
                            cone.discard();
                            return true;
                        }
                    }
                    return true;
                } else {
                    entry.setValue(age + 1);
                    return false;
                }
            });
        }
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
        if (event.getEntity() instanceof Player player) {
            Skill skill = RegistrySkills.SKILLS_REGISTRY.get().getValue(new ResourceLocation("otherworldorigins", "wisdom"));
            if (skill != null && skill.isEnabled(player)) {
                event.addLevels(1);
            }
        }
    }

    public static void trackConeProjectile(AbstractConeProjectile coneProjectile) {
        activeCones.put(coneProjectile.getId(), 0);
    }

    // Reprompting manually with our own checks, server-side only -
    // Origins Forge seems to have some race conditions with synchronization
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        if (!Apotheosis.enableAdventure){
            serverPlayer.sendSystemMessage(
                    Component.literal("Otherworld Origins requires the Apotheosis " +
                            "Adventure Module enabled to work correctly. Please enable it " +
                            "in /config/apotheosis/apotheosis.cfg, or you will be missing origins!"),
                    true);
        }

        OriginComponent originComponent = io.github.apace100.origins.registry.ModComponents.ORIGIN.maybeGet(player).orElse(null);
        if (originComponent == null) return;

        List<OriginLayer> missingOriginLayers = new ArrayList<>();

        for (OriginLayer layer : OriginLayers.getLayers()) {
            if (!layer.isEnabled()) continue;

            Origin currentOrigin = originComponent.getOrigin(layer);
            if (currentOrigin == null || currentOrigin == Origin.EMPTY) {
                List<ResourceLocation> availableOrigins = layer.getOrigins(player);
                if (!availableOrigins.isEmpty()) {
                    missingOriginLayers.add(layer);
                }
            }
        }

        if (!missingOriginLayers.isEmpty()) {
            PacketDistributor.PacketTarget target = PacketDistributor.PLAYER.with(() -> serverPlayer);
            OtherworldOrigins.CHANNEL.send(target, new CloseCurrentScreenMessage());
            originComponent.sync();
            OtherworldOrigins.CHANNEL.send(target, new OpenOriginScreenMessage(false));
        }
    }

    @SubscribeEvent
    public static void onCriticalHit(CriticalHitEvent event) {
        Player player = event.getEntity();

        if (event.getTarget() instanceof Player targetPlayer) {
            if (io.github.apace100.apoli.component.PowerHolderComponent.hasPower(targetPlayer, dev.muon.otherworldorigins.power.PreventCriticalHitPower.class)) {
                event.setDamageModifier(1.0f);
                event.setResult(CriticalHitEvent.Result.DENY);
                return;
            }
        }

        double totalModifier = io.github.apace100.apoli.component.PowerHolderComponent.getPowers(player, ModifyCriticalHitPower.class).stream()
                .filter(ModifyCriticalHitPower::isActive)
                .mapToDouble(powerType -> ModifierUtil.applyModifiers(player, powerType.getModifiers(), 0.0))
                .sum();

        if (totalModifier != 0) {
            float newDamageModifier = event.getDamageModifier() * (1 + (float) totalModifier);
            event.setDamageModifier(newDamageModifier);
            event.setResult(CriticalHitEvent.Result.ALLOW);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();

        if (!(attacker instanceof Player player)) {
            return;
        }

        ItemStack weapon = player.getMainHandItem();
        float damageReduction = 0;

        if (!EnchantmentRestrictions.isEnchantmentAllowed(player, Enchantments.SHARPNESS)) {
            damageReduction += calculateDamageBonus(weapon, Enchantments.SHARPNESS, MobType.UNDEFINED);
        }
        if (!EnchantmentRestrictions.isEnchantmentAllowed(player, Enchantments.SMITE)) {
            damageReduction += calculateDamageBonus(weapon, Enchantments.SMITE, MobType.UNDEAD);
        }
        if (!EnchantmentRestrictions.isEnchantmentAllowed(player, Enchantments.BANE_OF_ARTHROPODS)) {
            damageReduction += calculateDamageBonus(weapon, Enchantments.BANE_OF_ARTHROPODS, MobType.ARTHROPOD);
        }

        if (damageReduction > 0) {
            event.setAmount(Math.max(0, event.getAmount() - damageReduction));
        }
    }

    private static float calculateDamageBonus(ItemStack weapon, Enchantment enchantment, MobType mobType) {
        int level = weapon.getEnchantmentLevel(enchantment);
        if (level > 0) {
            if (mobType == MobType.UNDEFINED) {
                return 1 + level * 0.5F;
            } else {
                return level * 1.5F;
            }
        }
        return 0;
    }
}


