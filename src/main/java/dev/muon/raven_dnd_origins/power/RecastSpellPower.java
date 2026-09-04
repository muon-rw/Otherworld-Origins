package dev.muon.raven_dnd_origins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.raven_dnd_origins.util.spell.SpellCastUtil;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * After a spell finishes ({@link AbstractSpell#onServerCastComplete}), may cast the same spell again via
 * {@link SpellCastUtil#castResolvedSpellForPlayer} after {@code delay_ticks} server ticks (default 5;
 * use {@code 0} for same-tick), using the same pipeline options as {@code cast_spell}.
 * <p>
 * Same {@link CastSource#COMMAND} default as {@link ActionOnSpellCastPower}: excluded unless
 * {@code cast_sources} is overridden (e.g. Wild Magic free recast with flat chance).
 */
public class RecastSpellPower extends PowerType<RecastSpellPower.Configuration> {

    private static final List<PendingRecast> PENDING = Collections.synchronizedList(new ArrayList<>());

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            CastSpellConditions.CODEC.optionalFieldOf("cast_conditions", CastSpellConditions.defaults())
                    .forGetter(Configuration::castConditions),
            Codec.BOOL.optionalFieldOf("skip_if_spell_has_recasts", true).forGetter(Configuration::skipIfSpellHasRecasts),
            Codec.INT.optionalFieldOf("delay_ticks", 5).forGetter(Configuration::delayTicks),
            Codec.INT.optionalFieldOf("cast_time").forGetter(Configuration::castTime),
            Codec.INT.optionalFieldOf("mana_cost").forGetter(Configuration::manaCost),
            Codec.BOOL.optionalFieldOf("continuous_cost", false).forGetter(Configuration::continuousCost),
            Codec.INT.optionalFieldOf("cost_interval", 20).forGetter(Configuration::costInterval),
            Codec.DOUBLE.optionalFieldOf("raycast_distance").forGetter(Configuration::raycastDistance),
            LoggedOptionalField.of("entity_action", EntityAction.CODEC).forGetter(Configuration::entityAction)
    ).apply(instance, Configuration::new));

    @Override
    public MapCodec<Configuration> configCodec() {
        return CODEC;
    }

    /**
     * Run from server tick; executes recasts whose delay has elapsed.
     * <p>
     * Due recasts are run <i>after</i> updating {@link #PENDING}: executing a recast can synchronously
     * finish another spell and enqueue a new pending recast, which would
     * {@link java.util.ConcurrentModificationException} if we still held an open iterator.
     */
    public static void tickPendingRecasts(MinecraftServer server) {
        List<PendingRecast> due = new ArrayList<>();
        synchronized (PENDING) {
            Iterator<PendingRecast> it = PENDING.iterator();
            while (it.hasNext()) {
                PendingRecast pending = it.next();
                pending.ticksRemaining--;
                if (pending.ticksRemaining <= 0) {
                    it.remove();
                    due.add(pending);
                }
            }
        }
        for (PendingRecast pending : due) {
            tryExecuteRecast(server, pending);
        }
    }

    private static void tryExecuteRecast(MinecraftServer server, PendingRecast pending) {
        ServerPlayer player = server.getPlayerList().getPlayer(pending.playerId);
        if (player == null || !player.isAlive()) {
            return;
        }
        AbstractSpell spell = SpellRegistry.getSpell(pending.spellId);
        if (spell == SpellRegistry.none()) {
            return;
        }
        boolean cast = SpellCastUtil.castResolvedSpellForPlayer(
                player,
                spell,
                pending.spellLevel,
                pending.castTime,
                pending.manaCost,
                pending.continuousCost,
                pending.costInterval,
                pending.raycastDistance
        );
        if (cast) {
            runEntityAction(pending.entityAction, player);
        }
    }

    public static void handleSpellCastComplete(
            AbstractSpell spell,
            LivingEntity caster,
            int spellLevel,
            CastSource castSource,
            CastType castType
    ) {
        if (caster.level().isClientSide()) {
            return;
        }
        if (!(caster instanceof ServerPlayer serverPlayer)) {
            return;
        }
        PowerLookup.forEach(caster, ModPowers.RECAST_SPELL, Configuration.class, config ->
                triggerIfMatch(config, spell, serverPlayer, spellLevel, castSource, castType));
    }

    private static void triggerIfMatch(
            Configuration config,
            AbstractSpell spell,
            ServerPlayer caster,
            int spellLevel,
            CastSource castSource,
            CastType castType
    ) {
        if (!config.castConditions().matches(spell, castSource, castType)) {
            return;
        }
        if (config.skipIfSpellHasRecasts() && spell.getRecastCount(spellLevel, caster) > 0) {
            return;
        }
        int delay = config.delayTicks();
        if (delay <= 0) {
            performRecastCast(caster, spell, spellLevel, config);
        } else {
            synchronized (PENDING) {
                PENDING.add(PendingRecast.schedule(caster.getUUID(), spell.getSpellResource(), spellLevel, delay, config));
            }
        }
    }

    private static void performRecastCast(ServerPlayer player, AbstractSpell spell, int spellLevel, Configuration config) {
        boolean cast = SpellCastUtil.castResolvedSpellForPlayer(
                player,
                spell,
                spellLevel,
                config.castTime(),
                config.manaCost(),
                config.continuousCost(),
                config.costInterval(),
                config.raycastDistance()
        );
        if (cast) {
            runEntityAction(config.entityAction(), player);
        }
    }

    private static void runEntityAction(Optional<EntityAction> action, ServerPlayer player) {
        action.ifPresent(a -> a.run(new EntityCtx(player, player.level())));
    }

    private static final class PendingRecast {
        final UUID playerId;
        final ResourceLocation spellId;
        final int spellLevel;
        int ticksRemaining;
        final Optional<Integer> castTime;
        final Optional<Integer> manaCost;
        final boolean continuousCost;
        final int costInterval;
        final Optional<Double> raycastDistance;
        final Optional<EntityAction> entityAction;

        private PendingRecast(
                UUID playerId,
                ResourceLocation spellId,
                int spellLevel,
                int ticksRemaining,
                Optional<Integer> castTime,
                Optional<Integer> manaCost,
                boolean continuousCost,
                int costInterval,
                Optional<Double> raycastDistance,
                Optional<EntityAction> entityAction
        ) {
            this.playerId = playerId;
            this.spellId = spellId;
            this.spellLevel = spellLevel;
            this.ticksRemaining = ticksRemaining;
            this.castTime = castTime;
            this.manaCost = manaCost;
            this.continuousCost = continuousCost;
            this.costInterval = costInterval;
            this.raycastDistance = raycastDistance;
            this.entityAction = entityAction;
        }

        static PendingRecast schedule(
                UUID playerId,
                ResourceLocation spellId,
                int spellLevel,
                int delayTicks,
                Configuration config
        ) {
            return new PendingRecast(
                    playerId,
                    spellId,
                    spellLevel,
                    delayTicks,
                    config.castTime(),
                    config.manaCost(),
                    config.continuousCost(),
                    config.costInterval(),
                    config.raycastDistance(),
                    config.entityAction()
            );
        }
    }

    public record Configuration(
            CastSpellConditions castConditions,
            boolean skipIfSpellHasRecasts,
            int delayTicks,
            Optional<Integer> castTime,
            Optional<Integer> manaCost,
            boolean continuousCost,
            int costInterval,
            Optional<Double> raycastDistance,
            Optional<EntityAction> entityAction
    ) {
    }
}
