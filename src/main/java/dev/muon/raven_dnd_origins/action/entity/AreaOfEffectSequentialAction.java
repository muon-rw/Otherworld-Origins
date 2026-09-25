package dev.muon.raven_dnd_origins.action.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.action.DelayedActionQueue;
import dev.overgrown.apoli.alias.AliasingMapCodec;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Shape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Like Apoli {@code area_of_effect}: gathers entities in a radius, optional {@code bientity_condition},
 * {@code include_actor} (when {@code false}, the actor is not a candidate), and {@code shape} in the codec
 * for datapack parity (shape is not otherwise applied, matching the original implementation).
 * <p>
 * Differs by sorting targets by distance to the actor (nearest first), then running {@code bientity_action}
 * {@code application_count} times on that ordered list, cycling with {@code index % list.size()} when there
 * are fewer targets than applications. Optional {@code delay_ticks} waits that many server ticks between
 * applications (the first runs immediately when the action executes), using Apoli's own
 * {@link DelayedActionQueue} to schedule the remaining shots.
 */
public final class AreaOfEffectSequentialAction implements ActionType<EntityCtx, AreaOfEffectSequentialAction.Cfg> {

    private static final String LOG_PREFIX = "area_of_effect_sequential";

    public record Cfg(
            double radius,
            BiEntityAction bientityAction,
            Optional<BiEntityCondition> bientityCondition,
            Shape shape,
            boolean includeActor,
            int delayTicks,
            int applicationCount
    ) {}

    private static final MapCodec<Cfg> INNER = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.optionalFieldOf("radius", 16.0).forGetter(Cfg::radius),
            BiEntityAction.CODEC.fieldOf("bientity_action").forGetter(Cfg::bientityAction),
            BiEntityCondition.CODEC.optionalFieldOf("bientity_condition").forGetter(Cfg::bientityCondition),
            Shape.CODEC.optionalFieldOf("shape", Shape.CUBE).forGetter(Cfg::shape),
            Codec.BOOL.optionalFieldOf("include_actor", false).forGetter(Cfg::includeActor),
            Codec.INT.optionalFieldOf("delay_ticks", 0).forGetter(Cfg::delayTicks),
            Codec.INT.optionalFieldOf("application_count", 1).forGetter(Cfg::applicationCount)
    ).apply(i, Cfg::new));

    @Override
    public MapCodec<Cfg> codec() {
        return AliasingMapCodec.wrap(INNER, Map.of("include_target", "include_actor"));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        if (ctx.level().isClientSide()) {
            return;
        }
        Entity entity = ctx.entity();
        int applications = Math.max(1, cfg.applicationCount());
        int delayTicks = Math.max(0, cfg.delayTicks());
        RavenDndOrigins.LOGGER.debug(
                "{}: execute start actor={} ({}) dim={} pos=({}, {}, {}) radius={} includeActor={} applicationCount={} delayTicks={}",
                LOG_PREFIX,
                entity.getUUID(),
                entity.getType().getDescription().getString(),
                entity.level().dimension().location(),
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                cfg.radius(),
                cfg.includeActor(),
                applications,
                delayTicks
        );

        List<UUID> targetOrder = collectTargetIds(cfg, entity);
        if (targetOrder.isEmpty()) {
            RavenDndOrigins.LOGGER.debug(
                    "{}: no valid targets — bientity_action will not run (see previous DEBUG line for filter breakdown)",
                    LOG_PREFIX
            );
            return;
        }

        RavenDndOrigins.LOGGER.debug(
                "{}: {} target UUID(s) after filters; first application uses {}",
                LOG_PREFIX,
                targetOrder.size(),
                targetOrder.get(0)
        );

        if (delayTicks == 0) {
            for (int i = 0; i < applications; i++) {
                UUID tid = targetOrder.get(i % targetOrder.size());
                Entity target = resolveEntity(ctx.level(), tid);
                if (target == null) {
                    RavenDndOrigins.LOGGER.debug("{}: sync shot {} could not resolve target UUID {}", LOG_PREFIX, i, tid);
                    continue;
                }
                RavenDndOrigins.LOGGER.debug(
                        "{}: sync bientity_action shot {} actor={} target={} ({})",
                        LOG_PREFIX, i, entity.getUUID(), target.getUUID(), target.getType().getDescription().getString()
                );
                cfg.bientityAction().run(BiEntityCtx.of(entity, target, ctx.level()));
            }
            return;
        }

        Entity firstTarget = resolveEntity(ctx.level(), targetOrder.get(0));
        if (firstTarget == null) {
            RavenDndOrigins.LOGGER.debug("{}: delayed mode — could not resolve first target UUID {}", LOG_PREFIX, targetOrder.get(0));
        } else {
            RavenDndOrigins.LOGGER.debug(
                    "{}: immediate first shot actor={} target={} ({})",
                    LOG_PREFIX, entity.getUUID(), firstTarget.getUUID(), firstTarget.getType().getDescription().getString()
            );
            cfg.bientityAction().run(BiEntityCtx.of(entity, firstTarget, ctx.level()));
        }
        if (applications <= 1) {
            return;
        }
        if (!(ctx.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        scheduleShot(cfg, entity.getUUID(), serverLevel, targetOrder, 1, applications - 1, delayTicks);
        RavenDndOrigins.LOGGER.debug(
                "{}: scheduled {} delayed shot(s), delayTicks={}", LOG_PREFIX, applications - 1, delayTicks
        );
    }

    private static void scheduleShot(
            Cfg cfg, UUID casterId, ServerLevel level, List<UUID> targetOrder,
            int nextIndex, int remaining, int delayTicks
    ) {
        DelayedActionQueue.schedule(delayTicks, DelayedActionQueue.ALWAYS, () ->
                runDueShot(cfg, casterId, level, targetOrder, nextIndex, remaining, delayTicks));
    }

    private static void runDueShot(
            Cfg cfg, UUID casterId, ServerLevel level, List<UUID> targetOrder,
            int index, int remaining, int delayTicks
    ) {
        Entity caster = resolveEntity(level, casterId);
        if (caster == null || !caster.isAlive()) {
            RavenDndOrigins.LOGGER.debug("{}: delayed shot dropped — caster {} missing or dead", LOG_PREFIX, casterId);
            return;
        }
        UUID targetId = targetOrder.get(index % targetOrder.size());
        Entity target = resolveEntity(level, targetId);
        if (target == null) {
            RavenDndOrigins.LOGGER.debug("{}: delayed shot {} — could not resolve target UUID {}", LOG_PREFIX, index, targetId);
        } else {
            RavenDndOrigins.LOGGER.debug(
                    "{}: delayed bientity_action shot index={} actor={} target={} ({}) remainingAfterThis={}",
                    LOG_PREFIX, index, caster.getUUID(), target.getUUID(), target.getType().getDescription().getString(), remaining - 1
            );
            cfg.bientityAction().run(BiEntityCtx.of(caster, target, level));
        }
        if (remaining - 1 > 0) {
            scheduleShot(cfg, casterId, level, targetOrder, index + 1, remaining - 1, delayTicks);
        }
    }

    private static List<UUID> collectTargetIds(Cfg cfg, Entity entity) {
        double diameter = cfg.radius() * 2.0;
        List<Entity> found = new ArrayList<>();
        int scanned = 0;
        int skippedSelf = 0;
        int failedBientityCondition = 0;
        int outsideSphere = 0;
        for (Entity check : entity.level().getEntitiesOfClass(Entity.class, AABB.ofSize(entity.getPosition(1.0F), diameter, diameter, diameter))) {
            scanned++;
            if (check == entity && !cfg.includeActor()) {
                skippedSelf++;
                continue;
            }
            if (cfg.bientityCondition().isPresent()
                    && !cfg.bientityCondition().get().test(BiEntityCtx.of(entity, check, entity.level()))) {
                failedBientityCondition++;
                continue;
            }
            if (!(check.distanceToSqr(entity) < Mth.square(cfg.radius()))) {
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
        RavenDndOrigins.LOGGER.debug(
                "{}: scan in box scanned={} accepted={} skippedSelf={} failedBientityCondition={} outsideSphere={} (radius={})",
                LOG_PREFIX, scanned, found.size(), skippedSelf, failedBientityCondition, outsideSphere, cfg.radius()
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
}
