package dev.muon.otherworldorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.otherworldorigins.util.spell.SpellCastUtil;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredEntityAction;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import io.github.edwinmindcraft.calio.api.network.CalioCodecHelper;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import net.minecraft.core.Holder;
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
 * After a spell finishes ({@link io.redspace.ironsspellbooks.api.spells.AbstractSpell#onServerCastComplete}),
 * may cast the same spell again via {@link SpellCastUtil#castResolvedSpellForPlayer}
 * after {@code delay_ticks} server ticks (default 5; use {@code 0} for same-tick),
 * using the same pipeline options as {@code cast_spell}.
 * <p>
 * Same {@link CastSource#COMMAND} default as {@link ActionOnSpellCastPower}: excluded unless
 * {@code cast_sources} is overridden (e.g. Wild Magic free recast with flat chance).
 */
public class RecastSpellPower extends PowerFactory<RecastSpellPower.Configuration> {

    private static final List<PendingRecast> PENDING = Collections.synchronizedList(new ArrayList<>());

    public RecastSpellPower() {
        super(Configuration.CODEC);
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
            ConfiguredEntityAction.execute(pending.entityAction, player);
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
        IPowerContainer.getPowers(caster, ModPowers.RECAST_SPELL.get()).forEach(holder ->
                triggerIfMatch(holder.value(), spell, caster, spellLevel, castSource, castType)
        );
    }

    private static void triggerIfMatch(
            ConfiguredPower<Configuration, ?> power,
            AbstractSpell spell,
            LivingEntity caster,
            int spellLevel,
            CastSource castSource,
            CastType castType
    ) {
        if (!(caster instanceof ServerPlayer serverPlayer)) {
            return;
        }
        Configuration config = power.getConfiguration();
        if (!config.castConditions().matches(spell, castSource, castType)) {
            return;
        }
        if (config.skipIfSpellHasRecasts() && spell.getRecastCount(spellLevel, caster) > 0) {
            return;
        }
        int delay = config.delayTicks();
        if (delay <= 0) {
            performRecastCast(serverPlayer, spell, spellLevel, config);
        } else {
            synchronized (PENDING) {
                PENDING.add(PendingRecast.schedule(serverPlayer.getUUID(), spell.getSpellResource(), spellLevel, delay, config));
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
            ConfiguredEntityAction.execute(config.entityAction(), player);
        }
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
        final Holder<ConfiguredEntityAction<?, ?>> entityAction;

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
                Holder<ConfiguredEntityAction<?, ?>> entityAction
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
            Holder<ConfiguredEntityAction<?, ?>> entityAction
    ) implements IDynamicFeatureConfiguration {

        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                CastSpellConditions.CODEC.optionalFieldOf("cast_conditions", CastSpellConditions.defaults())
                        .forGetter(Configuration::castConditions),
                CalioCodecHelper.optionalField(CalioCodecHelper.BOOL, "skip_if_spell_has_recasts", true)
                        .forGetter(Configuration::skipIfSpellHasRecasts),
                CalioCodecHelper.optionalField(CalioCodecHelper.INT, "delay_ticks", 5)
                        .forGetter(Configuration::delayTicks),
                Codec.INT.optionalFieldOf("cast_time").forGetter(Configuration::castTime),
                Codec.INT.optionalFieldOf("mana_cost").forGetter(Configuration::manaCost),
                CalioCodecHelper.optionalField(CalioCodecHelper.BOOL, "continuous_cost", false)
                        .forGetter(Configuration::continuousCost),
                CalioCodecHelper.optionalField(CalioCodecHelper.INT, "cost_interval", 20)
                        .forGetter(Configuration::costInterval),
                Codec.DOUBLE.optionalFieldOf("raycast_distance").forGetter(Configuration::raycastDistance),
                ConfiguredEntityAction.optional("entity_action").forGetter(Configuration::entityAction)
        ).apply(instance, Configuration::new));

        @Override
        public boolean isConfigurationValid() {
            return true;
        }
    }
}
