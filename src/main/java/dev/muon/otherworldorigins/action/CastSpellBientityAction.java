package dev.muon.otherworldorigins.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.otherworldorigins.OtherworldOrigins;
import io.github.apace100.calio.data.SerializableDataTypes;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.BiEntityAction;
import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.api.events.ChangeManaEvent;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastResult;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.network.casting.CastErrorPacket;
import io.redspace.ironsspellbooks.network.casting.OnCastStartedPacket;
import io.redspace.ironsspellbooks.network.casting.OnClientCastPacket;
import io.redspace.ironsspellbooks.network.casting.SyncTargetingDataPacket;
import io.redspace.ironsspellbooks.network.casting.UpdateCastingStatePacket;
import io.redspace.ironsspellbooks.setup.PacketDistributor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.entity.PartEntity;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * A bi-entity action that casts a spell from the actor towards the target.
 * Unlike CastSpellAction, this does not raycast - the target is provided directly.
 */
public class CastSpellBientityAction extends BiEntityAction<CastSpellBientityAction.Configuration> {

    public CastSpellBientityAction() {
        super(Configuration.CODEC);
    }

    @Override
    public void execute(Configuration configuration, Entity actor, Entity target) {
        if (!(actor instanceof LivingEntity caster)) {
            OtherworldOrigins.LOGGER.debug("CastSpellBientityAction: Actor is not a LivingEntity: {}", actor);
            return;
        }

        if (!(target instanceof LivingEntity livingTarget)) {
            OtherworldOrigins.LOGGER.debug("CastSpellBientityAction: Target is not a LivingEntity: {}", target);
            return;
        }

        String spellStr = configuration.spell().toString();
        ResourceLocation spellResourceLocation = ResourceLocation.tryParse(spellStr);
        if (spellResourceLocation != null && spellResourceLocation.getNamespace().equals("minecraft")) {
            spellResourceLocation = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", spellResourceLocation.getPath());
        }

        AbstractSpell spell = SpellRegistry.getSpell(spellResourceLocation);
        if (spell == null || "none".equals(spell.getSpellName())) {
            OtherworldOrigins.LOGGER.debug("CastSpellBientityAction: No valid spell found for resource location {}", spellResourceLocation);
            return;
        }

        Level world = actor.level();
        if (world.isClientSide) {
            return;
        }

        int powerLevel = configuration.powerLevel();
        MagicData magicData = MagicData.getPlayerMagicData(caster);

        // Handle case where entity is already casting - force completion of previous spell
        if (magicData.isCasting()) {
            OtherworldOrigins.LOGGER.debug("CastSpellBientityAction: Entity is still casting {}, forcing spell completion", magicData.getCastingSpellId());
            AbstractSpell oldSpell = magicData.getCastingSpell().getSpell();
            oldSpell.onCast(world, magicData.getCastingSpellLevel(), caster, magicData.getCastSource(), magicData);
            oldSpell.onServerCastComplete(world, magicData.getCastingSpellLevel(), caster, magicData, false);
            magicData.resetCastingState();
            magicData = MagicData.getPlayerMagicData(caster);
        }

        // Set up targeting data with the provided target
        OtherworldOrigins.LOGGER.debug("CastSpellBientityAction: Setting target to {}", livingTarget.getName().getString());
        updateTargetData(caster, livingTarget, magicData, spell, e -> true);

        if (caster instanceof ServerPlayer serverPlayer) {
            castSpellForPlayer(configuration, spell, powerLevel, serverPlayer, magicData, world);
        } else if (caster instanceof IMagicEntity magicEntity) {
            magicEntity.initiateCastSpell(spell, powerLevel);
        } else {
            // Non-player LivingEntity casting
            if (spell.checkPreCastConditions(world, powerLevel, caster, magicData)) {
                spell.onCast(world, powerLevel, caster, CastSource.COMMAND, magicData);
                spell.onServerCastComplete(world, powerLevel, caster, magicData, false);
            }
        }
    }

    private void castSpellForPlayer(Configuration configuration, AbstractSpell spell, int powerLevel,
                                     ServerPlayer serverPlayer, MagicData magicData, Level world) {
        Optional<Integer> castTimeOpt = configuration.castTime();
        Optional<Integer> manaCostOpt = configuration.manaCost();

        // Check cast conditions
        CastResult castResult = spell.canBeCastedBy(powerLevel, CastSource.COMMAND, magicData, serverPlayer);
        if (castResult.message != null) {
            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(castResult.message));
        }

        if (!castResult.isSuccess() ||
                !spell.checkPreCastConditions(world, powerLevel, serverPlayer, magicData) ||
                MinecraftForge.EVENT_BUS.post(new SpellPreCastEvent(serverPlayer, spell.getSpellId(), powerLevel, spell.getSchoolType(), CastSource.COMMAND))) {
            return;
        }

        // Handle mana cost
        if (manaCostOpt.isPresent()) {
            int manaCost = manaCostOpt.get();
            if (!serverPlayer.getAbilities().instabuild && magicData.getMana() < manaCost) {
                PacketDistributor.sendToPlayer(serverPlayer, CastErrorPacket.ErrorType.MANA);
                return;
            }
            if (!serverPlayer.getAbilities().instabuild) {
                setManaWithEvent(serverPlayer, magicData, magicData.getMana() - manaCost);
            }
        }

        if (serverPlayer.isUsingItem()) {
            serverPlayer.stopUsingItem();
        }

        // Determine effective cast time
        int effectiveCastTime;
        if (castTimeOpt.isPresent()) {
            effectiveCastTime = castTimeOpt.get();
        } else if (spell.getCastType() == CastType.CONTINUOUS) {
            effectiveCastTime = spell.getEffectiveCastTime(powerLevel, serverPlayer);
        } else if (spell.getCastType() == CastType.INSTANT) {
            effectiveCastTime = 0;
        } else {
            effectiveCastTime = spell.getEffectiveCastTime(powerLevel, serverPlayer);
        }

        // Set up continuous mana drain if configured
        if (configuration.continuousCost() && manaCostOpt.isPresent() && !serverPlayer.getAbilities().instabuild) {
            int manaCost = manaCostOpt.get();
            int costInterval = configuration.costInterval();
            CastSpellAction.registerContinuousCast(serverPlayer.getUUID(), manaCost, costInterval);
        }

        // Initiate the cast
        magicData.initiateCast(spell, powerLevel, effectiveCastTime, CastSource.COMMAND, "command");
        magicData.setPlayerCastingItem(ItemStack.EMPTY);

        spell.onServerPreCast(world, powerLevel, serverPlayer, magicData);

        // Sync casting state to client
        PacketDistributor.sendToPlayer(serverPlayer, new UpdateCastingStatePacket(spell.getSpellId(), powerLevel, effectiveCastTime, CastSource.COMMAND, "command"));
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(serverPlayer, new OnCastStartedPacket(serverPlayer.getUUID(), spell.getSpellId(), powerLevel));

        // Log target data if present
        if (magicData.getAdditionalCastData() instanceof TargetEntityCastData targetingData) {
            LivingEntity target = targetingData.getTarget((ServerLevel) serverPlayer.level());
            if (target != null) {
                OtherworldOrigins.LOGGER.debug("CastSpellBientityAction: Casting Spell {} with target {}", magicData.getCastingSpellId(), target.getName().getString());
            }
        }

        // For instant cast spells (effectiveCastTime == 0), execute immediately
        if (effectiveCastTime == 0) {
            spell.onCast(world, powerLevel, serverPlayer, CastSource.COMMAND, magicData);
            PacketDistributor.sendToPlayer(serverPlayer, new OnClientCastPacket(spell.getSpellId(), powerLevel, CastSource.COMMAND, magicData.getAdditionalCastData()));
        }
    }

    /**
     * Update the magic data with target information and sync to client.
     */
    private static void updateTargetData(LivingEntity caster, Entity entityHit, MagicData playerMagicData,
                                          AbstractSpell spell, Predicate<LivingEntity> filter) {
        LivingEntity livingTarget = null;

        if (entityHit instanceof LivingEntity livingEntity && filter.test(livingEntity)) {
            livingTarget = livingEntity;
        } else if (entityHit instanceof PartEntity<?> partEntity &&
                partEntity.getParent() instanceof LivingEntity livingParent &&
                filter.test(livingParent)) {
            livingTarget = livingParent;
        }

        if (livingTarget != null) {
            playerMagicData.setAdditionalCastData(new TargetEntityCastData(livingTarget));

            if (caster instanceof ServerPlayer serverPlayer) {
                if (spell.getCastType() != CastType.INSTANT) {
                    PacketDistributor.sendToPlayer(serverPlayer, new SyncTargetingDataPacket(livingTarget, spell));
                }
                serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(
                        Component.translatable("ui.irons_spellbooks.spell_target_success",
                                livingTarget.getDisplayName().getString(),
                                spell.getDisplayName(serverPlayer)).withStyle(ChatFormatting.GREEN)));
            }

            if (livingTarget instanceof ServerPlayer targetPlayer) {
                Utils.sendTargetedNotification(targetPlayer, caster, spell);
            }
        }
    }

    private static void setManaWithEvent(ServerPlayer player, MagicData magicData, float newMana) {
        ChangeManaEvent event = new ChangeManaEvent(player, magicData, magicData.getMana(), newMana);
        if (!MinecraftForge.EVENT_BUS.post(event)) {
            magicData.setMana(event.getNewMana());
        }
    }

    public static class Configuration implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                SerializableDataTypes.IDENTIFIER.fieldOf("spell").forGetter(Configuration::spell),
                Codec.INT.optionalFieldOf("power_level", 1).forGetter(Configuration::powerLevel),
                Codec.INT.optionalFieldOf("cast_time").forGetter(Configuration::castTime),
                Codec.INT.optionalFieldOf("mana_cost").forGetter(Configuration::manaCost),
                Codec.BOOL.optionalFieldOf("continuous_cost", false).forGetter(Configuration::continuousCost),
                Codec.INT.optionalFieldOf("cost_interval", 20).forGetter(Configuration::costInterval)
        ).apply(instance, Configuration::new));

        private final ResourceLocation spell;
        private final int powerLevel;
        private final Optional<Integer> castTime;
        private final Optional<Integer> manaCost;
        private final boolean continuousCost;
        private final int costInterval;

        public Configuration(ResourceLocation spell, int powerLevel, Optional<Integer> castTime,
                             Optional<Integer> manaCost, boolean continuousCost, int costInterval) {
            this.spell = spell;
            this.powerLevel = powerLevel;
            this.castTime = castTime;
            this.manaCost = manaCost;
            this.continuousCost = continuousCost;
            this.costInterval = costInterval;
        }

        public ResourceLocation spell() {
            return spell;
        }

        public int powerLevel() {
            return powerLevel;
        }

        public Optional<Integer> castTime() {
            return castTime;
        }

        public Optional<Integer> manaCost() {
            return manaCost;
        }

        public boolean continuousCost() {
            return continuousCost;
        }

        public int costInterval() {
            return costInterval;
        }
    }
}
