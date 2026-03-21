package dev.muon.otherworldorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.otherworldorigins.OtherworldOrigins;
import io.github.apace100.apoli.data.ApoliDataTypes;
import io.github.apace100.apoli.util.HudRender;
import io.github.edwinmindcraft.apoli.api.power.IActivePower;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.apoli.api.power.configuration.power.IActiveCooldownPowerConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.power.ActiveCooldownPowerFactory;
import io.github.edwinmindcraft.calio.api.network.CalioCodecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Teleports the player up to {@code distance} blocks along their look direction without requiring
 * a block raycast hit. Uses the exact aim point (including mid-air), not ground-snapping.
 * Grants fall damage immunity until the player touches the ground.
 */
public class DirectionalTeleportPower extends ActiveCooldownPowerFactory.Simple<DirectionalTeleportPower.Configuration> {

    private static final String FALL_IMMUNITY_TAG = "PsionicWarpFallImmune";
    private static final Map<UUID, PendingMirrorSound> PENDING_MIRROR_SOUNDS = new ConcurrentHashMap<>();

    public DirectionalTeleportPower() {
        super(Configuration.CODEC);
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
        return OtherworldOrigins.MODID + ":" + FALL_IMMUNITY_TAG;
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

    @Override
    protected void execute(ConfiguredPower<Configuration, ?> configuration, Entity entity) {
        if (!(entity instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        int distance = configuration.getConfiguration().distance();
        performTeleport(player, distance);
    }

    public static void performTeleport(ServerPlayer player, int maxDistance) {
        ServerLevel level = player.serverLevel();
        Vec3 start = player.position();
        Vec3 look = player.getLookAngle().normalize();

        spawnPortalBurst(level, start.add(0.0, 1.0, 0.0));

        double step = 0.5;
        for (double d = maxDistance; d >= step; d -= step) {
            Vec3 targetPoint = start.add(look.scale(d));
            double tx = targetPoint.x;
            double ty = targetPoint.y;
            double tz = targetPoint.z;

            var enderEvent = ForgeEventFactory.onEnderTeleport(player, tx, ty, tz);
            if (enderEvent.isCanceled()) {
                continue;
            }

            double ex = enderEvent.getTargetX();
            double ey = enderEvent.getTargetY();
            double ez = enderEvent.getTargetZ();

            if (tryTeleportToLookTarget(player, ex, ey, ez)) {
                grantFallImmunity(player);
                spawnPortalBurst(level, player.position().add(0.0, 1.0, 0.0));
                scheduleMirrorSound(player, player.position(), 2);
                return;
            }
        }
    }

    /**
     * Moves the player to the given feet position if unobstructed — including floating in open air.
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
        level.sendParticles(ParticleTypes.PORTAL,
                at.x, at.y, at.z,
                48,
                0.1, 0.2, 0.1,
                1.0);
    }

    public record Configuration(
            int duration,
            HudRender hudRender,
            IActivePower.Key key,
            int distance
    ) implements IActiveCooldownPowerConfiguration {

        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                CalioCodecHelper.optionalField(CalioCodecHelper.INT, "cooldown", 1).forGetter(Configuration::duration),
                CalioCodecHelper.optionalField(ApoliDataTypes.HUD_RENDER, "hud_render", HudRender.DONT_RENDER).forGetter(Configuration::hudRender),
                CalioCodecHelper.optionalField(IActivePower.Key.BACKWARD_COMPATIBLE_CODEC, "key", IActivePower.Key.PRIMARY).forGetter(Configuration::key),
                CalioCodecHelper.optionalField(CalioCodecHelper.INT, "distance", 50).forGetter(Configuration::distance)
        ).apply(instance, Configuration::new));

        @Override
        public boolean isConfigurationValid() {
            return true;
        }
    }
}
