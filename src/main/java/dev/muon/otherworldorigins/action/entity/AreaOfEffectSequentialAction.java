package dev.muon.otherworldorigins.action.entity;

import dev.muon.otherworldorigins.OtherworldOrigins;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.apace100.apoli.util.Shape;
import io.github.apace100.calio.data.SerializableDataType;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.configuration.MustBeBound;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredBiEntityAction;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredBiEntityCondition;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityAction;
import io.github.edwinmindcraft.calio.api.network.CalioCodecHelper;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Like Apoli {@code origins:area_of_effect}: gathers entities in a radius (with the same
 * sphere-in-cube test as {@link io.github.apace100.apoli.action.entity.AreaOfEffectAction}),
 * optional {@code bientity_condition}, {@code include_self} (same role as Apoli
 * {@code origins:area_of_effect}'s {@code include_target}: when {@code false}, the actor is not
 * a candidate), and {@code shape} in the codec for datapack parity (shape is not otherwise
 * applied — same as that action's implementation).
 * <p>
 * Differs by sorting targets by distance to the actor (nearest first), then running
 * {@code bientity_action} {@code application_count} times on that ordered list, cycling with
 * {@code index % list.size()} when there are fewer targets than applications. Optional
 * {@code delay_ticks} waits that many server ticks between applications (the first runs
 * immediately when the action executes).
 */
public class AreaOfEffectSequentialAction extends EntityAction<AreaOfEffectSequentialAction.Configuration> {

    private static final String LOG_PREFIX = "area_of_effect_sequential";

    private static final List<Pending> PENDING = Collections.synchronizedList(new ArrayList<>());

    public AreaOfEffectSequentialAction() {
        super(Configuration.CODEC);
    }

    /**
     * Drain due delayed applications; call from server tick (e.g. end phase).
     */
    public static void tickPending(MinecraftServer server) {
        List<Pending> due = new ArrayList<>();
        synchronized (PENDING) {
            Iterator<Pending> it = PENDING.iterator();
            while (it.hasNext()) {
                Pending pending = it.next();
                pending.ticksUntilNext--;
                if (pending.ticksUntilNext <= 0) {
                    it.remove();
                    due.add(pending);
                }
            }
        }
        for (Pending pending : due) {
            pending.runDueShot(server);
        }
    }

    public static void clearPending() {
        synchronized (PENDING) {
            PENDING.clear();
        }
    }

    @Override
    public void execute(@NotNull Configuration configuration, @NotNull Entity entity) {
        if (entity.level().isClientSide()) {
            return;
        }
        int applications = Math.max(1, configuration.applicationCount());
        int delayTicks = Math.max(0, configuration.delayTicks());
        OtherworldOrigins.LOGGER.debug(
                "{}: execute start actor={} ({}) dim={} pos=({}, {}, {}) radius={} includeSelf={} applicationCount={} delayTicks={}",
                LOG_PREFIX,
                entity.getUUID(),
                entity.getType().getDescription().getString(),
                entity.level().dimension().location(),
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                configuration.radius(),
                configuration.includeSelf(),
                applications,
                delayTicks
        );

        List<UUID> targetOrder = collectTargetIds(configuration, entity);
        if (targetOrder.isEmpty()) {
            OtherworldOrigins.LOGGER.debug(
                    "{}: no valid targets — bientity_action will not run (see previous DEBUG line for filter breakdown)",
                    LOG_PREFIX
            );
            return;
        }

        OtherworldOrigins.LOGGER.debug(
                "{}: {} target UUID(s) after filters; first application uses {}",
                LOG_PREFIX,
                targetOrder.size(),
                targetOrder.get(0)
        );

        Holder<ConfiguredBiEntityAction<?, ?>> action = configuration.bientityAction();

        if (delayTicks == 0) {
            for (int i = 0; i < applications; i++) {
                UUID tid = targetOrder.get(i % targetOrder.size());
                Entity target = resolveEntity(entity.level(), tid);
                if (target == null) {
                    OtherworldOrigins.LOGGER.debug(
                            "{}: sync shot {} could not resolve target UUID {}",
                            LOG_PREFIX,
                            i,
                            tid
                    );
                    continue;
                }
                OtherworldOrigins.LOGGER.debug(
                        "{}: sync bientity_action shot {} actor={} target={} ({})",
                        LOG_PREFIX,
                        i,
                        entity.getUUID(),
                        target.getUUID(),
                        target.getType().getDescription().getString()
                );
                ConfiguredBiEntityAction.execute(action, entity, target);
            }
            return;
        }

        Entity firstTarget = resolveEntity(entity.level(), targetOrder.get(0));
        if (firstTarget == null) {
            OtherworldOrigins.LOGGER.debug(
                    "{}: delayed mode — could not resolve first target UUID {}",
                    LOG_PREFIX,
                    targetOrder.get(0)
            );
        } else {
            OtherworldOrigins.LOGGER.debug(
                    "{}: immediate first shot actor={} target={} ({})",
                    LOG_PREFIX,
                    entity.getUUID(),
                    firstTarget.getUUID(),
                    firstTarget.getType().getDescription().getString()
            );
            ConfiguredBiEntityAction.execute(action, entity, firstTarget);
        }
        if (applications <= 1) {
            return;
        }
        synchronized (PENDING) {
            PENDING.add(new Pending(
                    entity.getUUID(),
                    entity.level().dimension(),
                    targetOrder,
                    action,
                    1,
                    applications - 1,
                    delayTicks
            ));
            OtherworldOrigins.LOGGER.debug(
                    "{}: enqueued {} delayed shot(s), delayTicks={} pendingQueueSize={}",
                    LOG_PREFIX,
                    applications - 1,
                    delayTicks,
                    PENDING.size()
            );
        }
    }

    private static List<UUID> collectTargetIds(Configuration configuration, Entity entity) {
        double diameter = configuration.radius() * 2.0;
        List<Entity> found = new ArrayList<>();
        int scanned = 0;
        int skippedSelf = 0;
        int failedBientityCondition = 0;
        int outsideSphere = 0;
        for (Entity check : entity.level().getEntitiesOfClass(Entity.class, AABB.ofSize(entity.getPosition(1.0F), diameter, diameter, diameter))) {
            scanned++;
            if (check == entity && !configuration.includeSelf()) {
                skippedSelf++;
                continue;
            }
            if (!ConfiguredBiEntityCondition.check(configuration.bientityCondition(), entity, check)) {
                failedBientityCondition++;
                continue;
            }
            if (!(check.distanceToSqr(entity) < Mth.square(configuration.radius()))) {
                outsideSphere++;
                continue;
            }
            found.add(check);
        }
        found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(entity)));
        List<UUID> ids = new ArrayList<>(found.size());
        for (Entity e : found) {
            ids.add(e.getUUID());
        }
        OtherworldOrigins.LOGGER.debug(
                "{}: scan in box scanned={} accepted={} skippedSelf={} failedBientityCondition={} outsideSphere={} (radius={})",
                LOG_PREFIX,
                scanned,
                found.size(),
                skippedSelf,
                failedBientityCondition,
                outsideSphere,
                configuration.radius()
        );
        return ids;
    }

    private static @Nullable Entity resolveEntity(Level level, UUID uuid) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity byUuid = serverLevel.getEntity(uuid);
        if (byUuid != null) {
            return byUuid;
        }
        return serverLevel.getServer().getPlayerList().getPlayer(uuid);
    }

    private static final class Pending {
        final UUID casterId;
        final ResourceKey<Level> dimension;
        final List<UUID> targetOrder;
        final Holder<ConfiguredBiEntityAction<?, ?>> action;
        int nextApplicationIndex;
        int remainingDelayedApplications;
        int ticksUntilNext;
        final int delayTicks;

        Pending(
                UUID casterId,
                ResourceKey<Level> dimension,
                List<UUID> targetOrder,
                Holder<ConfiguredBiEntityAction<?, ?>> action,
                int nextApplicationIndex,
                int remainingDelayedApplications,
                int delayTicks
        ) {
            this.casterId = casterId;
            this.dimension = dimension;
            this.targetOrder = targetOrder;
            this.action = action;
            this.nextApplicationIndex = nextApplicationIndex;
            this.remainingDelayedApplications = remainingDelayedApplications;
            this.delayTicks = delayTicks;
            this.ticksUntilNext = delayTicks;
        }

        void runDueShot(MinecraftServer server) {
            ServerLevel level = server.getLevel(this.dimension);
            if (level == null) {
                OtherworldOrigins.LOGGER.debug(
                        "{}: delayed shot dropped — dimension {} not loaded",
                        LOG_PREFIX,
                        dimension.location()
                );
                return;
            }
            Entity caster = resolveEntity(level, casterId);
            if (caster == null || !caster.isAlive()) {
                OtherworldOrigins.LOGGER.debug(
                        "{}: delayed shot dropped — caster {} missing or dead (alive={})",
                        LOG_PREFIX,
                        casterId,
                        caster != null && caster.isAlive()
                );
                return;
            }
            UUID targetId = targetOrder.get(nextApplicationIndex % targetOrder.size());
            Entity target = resolveEntity(level, targetId);
            if (target == null) {
                OtherworldOrigins.LOGGER.debug(
                        "{}: delayed shot {} — could not resolve target UUID {}",
                        LOG_PREFIX,
                        nextApplicationIndex,
                        targetId
                );
            } else {
                OtherworldOrigins.LOGGER.debug(
                        "{}: delayed bientity_action shot index={} actor={} target={} ({}) remainingAfterThis={}",
                        LOG_PREFIX,
                        nextApplicationIndex,
                        caster.getUUID(),
                        target.getUUID(),
                        target.getType().getDescription().getString(),
                        remainingDelayedApplications - 1
                );
                ConfiguredBiEntityAction.execute(action, caster, target);
            }
            nextApplicationIndex++;
            remainingDelayedApplications--;
            if (remainingDelayedApplications > 0) {
                ticksUntilNext = delayTicks;
                synchronized (PENDING) {
                    PENDING.add(this);
                }
            }
        }
    }

    public record Configuration(
            double radius,
            @MustBeBound Holder<ConfiguredBiEntityAction<?, ?>> bientityAction,
            Holder<ConfiguredBiEntityCondition<?, ?>> bientityCondition,
            Shape shape,
            boolean includeSelf,
            int delayTicks,
            int applicationCount
    ) implements IDynamicFeatureConfiguration {

        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                CalioCodecHelper.optionalField(CalioCodecHelper.DOUBLE, "radius", 16.0).forGetter(Configuration::radius),
                ConfiguredBiEntityAction.required("bientity_action").forGetter(Configuration::bientityAction),
                ConfiguredBiEntityCondition.optional("bientity_condition").forGetter(Configuration::bientityCondition),
                CalioCodecHelper.optionalField(SerializableDataType.enumValue(Shape.class), "shape", Shape.CUBE).forGetter(Configuration::shape),
                CalioCodecHelper.optionalField(CalioCodecHelper.BOOL, "include_self", false).forGetter(Configuration::includeSelf),
                CalioCodecHelper.optionalField(CalioCodecHelper.INT, "delay_ticks", 0).forGetter(Configuration::delayTicks),
                CalioCodecHelper.optionalField(CalioCodecHelper.INT, "application_count", 1).forGetter(Configuration::applicationCount)
        ).apply(instance, Configuration::new));

        @Override
        public boolean isConfigurationValid() {
            return applicationCount >= 1 && delayTicks >= 0;
        }
    }
}
