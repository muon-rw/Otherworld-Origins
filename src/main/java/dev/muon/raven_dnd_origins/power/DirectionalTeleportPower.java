package dev.muon.raven_dnd_origins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.data.HudRender;
import dev.overgrown.apoli.data.Key;
import dev.overgrown.apoli.keybind.KeyDispatch;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerContainerImpl;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerResources;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Teleports the player up to {@code distance} blocks along their look direction without requiring
 * a block raycast hit. Uses the exact aim point (including mid-air), not ground-snapping.
 * Grants fall damage immunity until the player touches the ground.
 * <p>
 * Apoli polls keys and routes activations through closed instanceof chains over its own active power
 * types, so the press is offered here by
 * {@link dev.muon.raven_dnd_origins.mixin.client.compat.apoli.ApoliKeyHandlerActivationMixin} and
 * {@link dev.muon.raven_dnd_origins.mixin.compat.apoli.PowerActivationDispatchMixin} calling
 * {@link #onKeyPressed}.
 */
public class DirectionalTeleportPower extends PowerType<DirectionalTeleportPower.Configuration> {

    private static final String FALL_IMMUNITY_TAG = "PsionicWarpFallImmune";
    private static final Map<UUID, PendingMirrorSound> PENDING_MIRROR_SOUNDS = new ConcurrentHashMap<>();

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Expression.INT_OR_EXPR.optionalFieldOf("cooldown", Expression.constant(1)).forGetter(Configuration::cooldown),
            HudRender.CODEC.optionalFieldOf("hud_render", HudRender.DONT_RENDER).forGetter(Configuration::hudRender),
            Key.CODEC.optionalFieldOf("key", Key.DEFAULT_PRIMARY).forGetter(Configuration::key),
            Codec.INT.optionalFieldOf("distance", 50).forGetter(Configuration::distance)
    ).apply(instance, Configuration::new));

    @Override
    public MapCodec<Configuration> configCodec() {
        return CODEC;
    }

    @Override
    public void onAdded(ResourceLocation powerId, Configuration cfg, PowerContainer holder, ResourceLocation source) {
        if (holder instanceof PowerContainerImpl impl && impl.getAuxInt(powerId).isEmpty()) {
            impl.setAuxInt(powerId, 0);
        }
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Configuration cfg, PowerContainer holder, ResourceLocation source) {
        if (holder instanceof PowerContainerImpl impl && !holder.hasPower(powerId)) {
            impl.removeAux(powerId);
        }
    }

    @Override
    public OptionalInt readResource(ResourceLocation powerId, Configuration cfg, PowerContainer holder) {
        return PowerResources.readDeadline(holder, powerId);
    }

    @Override
    public OptionalInt writeResource(ResourceLocation powerId, Configuration cfg, PowerContainer holder, int value) {
        return PowerResources.writeDeadline(holder, powerId, value, PowerResources.cooldownTicks(cfg.cooldown(), holder));
    }

    @Override
    public OptionalInt resourceBound(ResourceLocation powerId, Configuration cfg, PowerContainer holder, boolean max) {
        return OptionalInt.of(max ? Math.max(PowerResources.cooldownTicks(cfg.cooldown(), holder), 0) : 0);
    }

    public static int onKeyPressed(Entity entity, String key) {
        if (!(entity instanceof ServerPlayer player) || player.level().isClientSide()) {
            return 0;
        }
        if (!(PowerContainer.of(player) instanceof PowerContainerImpl impl)) {
            return 0;
        }
        if (impl.powersOfType(ModPowers.DIRECTIONAL_TELEPORT).isEmpty() || KeyDispatch.blocked(player, key)) {
            return 0;
        }
        int[] fired = {0};
        PowerLookup.forEachEntry(player, ModPowers.DIRECTIONAL_TELEPORT, Configuration.class, (powerId, cfg) -> {
            if (!cfg.key().key().equals(key)) {
                return;
            }
            if (PowerResources.readDeadline(impl, powerId).orElse(0) > 0) {
                return;
            }
            performTeleport(player, cfg.distance());
            int cooldown = Math.max(PowerResources.cooldownTicks(cfg.cooldown(), impl), 0);
            PowerResources.writeDeadline(impl, powerId, cooldown, cooldown);
            fired[0]++;
        });
        return fired[0];
    }

    public static boolean hasFallImmunity(ServerPlayer player) {
        return player.getPersistentData().getBoolean(tagKey());
    }

    public static void grantFallImmunity(ServerPlayer player) {
        player.getPersistentData().putBoolean(tagKey(), true);
    }

    public static void clearFallImmunity(ServerPlayer player) {
        player.getPersistentData().remove(tagKey());
    }

    private static String tagKey() {
        return RavenDndOrigins.MODID + ":" + FALL_IMMUNITY_TAG;
    }

    /**
     * Plays {@link SoundEvents#ILLUSIONER_MIRROR_MOVE} at {@code position} after {@code delayTicks} server ticks.
     */
    public static void scheduleMirrorSound(ServerPlayer player, Vec3 position, int delayTicks) {
        PENDING_MIRROR_SOUNDS.put(player.getUUID(), new PendingMirrorSound(player.serverLevel(), position, delayTicks));
    }

    public static void tickPendingMirrorSounds() {
        Iterator<Map.Entry<UUID, PendingMirrorSound>> it = PENDING_MIRROR_SOUNDS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, PendingMirrorSound> entry = it.next();
            PendingMirrorSound pending = entry.getValue();
            pending.ticksLeft--;
            if (pending.ticksLeft <= 0) {
                pending.level.playSound(null, pending.position.x, pending.position.y, pending.position.z,
                        SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0F, 1.0F);
                it.remove();
            }
        }
    }

    private static final class PendingMirrorSound {
        final ServerLevel level;
        final Vec3 position;
        int ticksLeft;

        PendingMirrorSound(ServerLevel level, Vec3 position, int ticksLeft) {
            this.level = level;
            this.position = position;
            this.ticksLeft = ticksLeft;
        }
    }

    public static void performTeleport(ServerPlayer player, int maxDistance) {
        ServerLevel level = player.serverLevel();
        Vec3 start = player.position();
        Vec3 look = player.getLookAngle().normalize();

        spawnPortalBurst(level, start.add(0.0, 1.0, 0.0));

        double step = 0.5;
        for (double d = maxDistance; d >= step; d -= step) {
            Vec3 targetPoint = start.add(look.scale(d));
            EntityTeleportEvent.EnderEntity enderEvent =
                    EventHooks.onEnderTeleport(player, targetPoint.x, targetPoint.y, targetPoint.z);
            if (enderEvent.isCanceled()) {
                continue;
            }

            if (tryTeleportToLookTarget(player, enderEvent.getTargetX(), enderEvent.getTargetY(), enderEvent.getTargetZ())) {
                grantFallImmunity(player);
                spawnPortalBurst(level, player.position().add(0.0, 1.0, 0.0));
                scheduleMirrorSound(player, player.position(), 2);
                return;
            }
        }
    }

    /**
     * Moves the player to the given feet position if unobstructed, including floating in open air.
     */
    private static boolean tryTeleportToLookTarget(ServerPlayer player, double x, double y, double z) {
        ServerLevel level = player.serverLevel();
        if (!level.hasChunkAt(BlockPos.containing(x, y, z))) {
            return false;
        }
        double dx = x - player.getX();
        double dy = y - player.getY();
        double dz = z - player.getZ();
        AABB moved = player.getBoundingBox().move(dx, dy, dz);
        if (!level.noCollision(player, moved)) {
            return false;
        }
        player.teleportTo(x, y, z);
        player.resetFallDistance();
        level.broadcastEntityEvent(player, (byte) 46);
        return true;
    }

    private static void spawnPortalBurst(ServerLevel level, Vec3 at) {
        level.sendParticles(ParticleTypes.PORTAL, at.x, at.y, at.z, 48, 0.1, 0.2, 0.1, 1.0);
    }

    public record Configuration(
            Expression cooldown,
            HudRender hudRender,
            Key key,
            int distance
    ) {}
}
