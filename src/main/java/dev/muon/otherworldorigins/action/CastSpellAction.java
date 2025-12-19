package dev.muon.otherworldorigins.action;

import dev.muon.otherworldorigins.OtherworldOrigins;
import io.github.apace100.apoli.power.factory.action.ActionFactory;
import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.calio.data.SerializableDataTypes;
import io.redspace.ironsspellbooks.api.events.ChangeManaEvent;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastResult;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.network.ClientboundCastErrorMessage;
import io.redspace.ironsspellbooks.network.ClientboundUpdateCastingState;
import io.redspace.ironsspellbooks.network.spell.ClientboundOnCastStarted;
import io.redspace.ironsspellbooks.setup.Messages;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CastSpellAction {
    private static final Map<UUID, ContinuousCastData> CONTINUOUS_CASTS = new HashMap<>();

    public static void action(SerializableData.Instance data, Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            OtherworldOrigins.LOGGER.info("Entity is not a LivingEntity: " + entity);
            return;
        }

        ResourceLocation spellResourceLocation = data.getId("spell");
        // No one should be using the minecraft namespace anyway, and this is simpler
        if ("minecraft".equals(spellResourceLocation.getNamespace())) {
            spellResourceLocation = new ResourceLocation("irons_spellbooks", spellResourceLocation.getPath());
        }

        AbstractSpell spell = SpellRegistry.getSpell(spellResourceLocation);
        if (spell == null || "none".equals(spell.getSpellName())) {
            OtherworldOrigins.LOGGER.info("No valid spell found for resource location " + spellResourceLocation);
            return;
        }

        Level world = entity.level();
        if (world.isClientSide) {
            return;
        }

        int powerLevel = data.getInt("power_level");
        Optional<Integer> castTimeOpt = data.isPresent("cast_time") ? Optional.of(data.getInt("cast_time")) : Optional.empty();
        Optional<Integer> manaCostOpt = data.isPresent("mana_cost") ? Optional.of(data.getInt("mana_cost")) : Optional.empty();
        boolean continuousCost = data.getBoolean("continuous_cost");
        int costInterval = data.getInt("cost_interval");

        if (entity instanceof ServerPlayer serverPlayer) {
            MagicData magicData = MagicData.getPlayerMagicData(serverPlayer);
            if (!magicData.isCasting()) {
                CastResult castResult = spell.canBeCastedBy(powerLevel, CastSource.COMMAND, magicData, serverPlayer);
                if (castResult.message != null) {
                    serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(castResult.message));
                }

                if (!castResult.isSuccess() || !spell.checkPreCastConditions(world, powerLevel, serverPlayer, magicData) || MinecraftForge.EVENT_BUS.post(new SpellPreCastEvent(serverPlayer, spell.getSpellId(), powerLevel, spell.getSchoolType(), CastSource.COMMAND))) {
                    return;
                }

                if (serverPlayer.isUsingItem()) {
                    serverPlayer.stopUsingItem();
                }

                int effectiveCastTime = spell.getEffectiveCastTime(powerLevel, serverPlayer);
                if (castTimeOpt.isPresent()) {
                    int castTime = castTimeOpt.get();
                    effectiveCastTime = castTime;
                }

                if (manaCostOpt.isPresent()) {
                    int manaCost = manaCostOpt.get();
                    if (!serverPlayer.getAbilities().instabuild && magicData.getMana() < manaCost) {
                        Messages.sendToPlayer(new ClientboundCastErrorMessage(ClientboundCastErrorMessage.ErrorType.MANA, spell), serverPlayer);
                        return;
                    }
                    if (!serverPlayer.getAbilities().instabuild) {
                        setManaWithEvent(serverPlayer, magicData, magicData.getMana() - manaCost);
                    }
                }

                if (continuousCost && manaCostOpt.isPresent() && !serverPlayer.getAbilities().instabuild) {
                    int manaCost = manaCostOpt.get();
                    CONTINUOUS_CASTS.put(serverPlayer.getUUID(), new ContinuousCastData(manaCost, costInterval, 0));
                }

                magicData.initiateCast(spell, powerLevel, effectiveCastTime, CastSource.COMMAND, "command");
                magicData.setPlayerCastingItem(serverPlayer.getItemInHand(InteractionHand.MAIN_HAND));

                spell.onServerPreCast(world, powerLevel, serverPlayer, magicData);
                Messages.sendToPlayer(new ClientboundUpdateCastingState(spell.getSpellId(), powerLevel, effectiveCastTime, CastSource.COMMAND, "command"), serverPlayer);
                Messages.sendToPlayersTrackingEntity(new ClientboundOnCastStarted(serverPlayer.getUUID(), spell.getSpellId(), powerLevel), serverPlayer, true);

            } else {
                Utils.serverSideCancelCast(serverPlayer);
            }
        } else {
            OtherworldOrigins.LOGGER.info("Entity is not a valid caster (currently only players can cast spells with this entity action): " + entity);
        }
    }

    public static void onSpellTick(ServerPlayer player, MagicData magicData) {
        UUID playerId = player.getUUID();
        ContinuousCastData data = CONTINUOUS_CASTS.get(playerId);
        if (data != null) {
            data.ticksElapsed++;
            if (data.ticksElapsed >= data.costInterval) {
                data.ticksElapsed = 0;
                if (magicData.getMana() >= data.manaCost) {
                    setManaWithEvent(player, magicData, magicData.getMana() - data.manaCost);
                    OtherworldOrigins.LOGGER.info("Draining mana: " + data.manaCost + ". Remaining mana: " + magicData.getMana());
                } else {
                    Utils.serverSideCancelCast(player);
                    CONTINUOUS_CASTS.remove(playerId);
                }
            }
        }
    }

    private static void setManaWithEvent(ServerPlayer player, MagicData magicData, float newMana) {
        ChangeManaEvent event = new ChangeManaEvent(player, magicData, magicData.getMana(), newMana);
        if (!MinecraftForge.EVENT_BUS.post(event)) {
            magicData.setMana(event.getNewMana());
        }
    }

    public static void onSpellEnd(ServerPlayer player) {
        CONTINUOUS_CASTS.remove(player.getUUID());
    }

    private static class ContinuousCastData {
        final int manaCost;
        final int costInterval;
        int ticksElapsed;

        ContinuousCastData(int manaCost, int costInterval, int ticksElapsed) {
            this.manaCost = manaCost;
            this.costInterval = costInterval;
            this.ticksElapsed = ticksElapsed;
        }
    }

    public static ActionFactory<Entity> getFactory() {
        return new ActionFactory<>(
                OtherworldOrigins.loc("cast_spell"),
                new SerializableData()
                        .add("spell", SerializableDataTypes.IDENTIFIER)
                        .add("power_level", SerializableDataTypes.INT, 1)
                        .add("cast_time", SerializableDataTypes.INT)
                        .add("mana_cost", SerializableDataTypes.INT)
                        .add("continuous_cost", SerializableDataTypes.BOOLEAN, false)
                        .add("cost_interval", SerializableDataTypes.INT, 20),
                CastSpellAction::action
        );
    }
}
