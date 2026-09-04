package dev.muon.raven_dnd_origins.client;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.client.shapeshift.FakeEntityCache;
import dev.muon.raven_dnd_origins.client.shapeshift.ShapeshiftClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Powered elytra flight for wild shapes that are {@link FlyingAnimal}s or are tagged
 * {@code #raven_dnd_origins:powered_flight_forms}. While the player is fall-flying in such a form,
 * continuous thrust is applied in the look direction, preventing stalling and enabling sustained
 * flight without rockets.
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = RavenDndOrigins.MODID)
public class EagleFlightHandler {

    public static final TagKey<EntityType<?>> POWERED_FLIGHT_FORMS =
            TagKey.create(Registries.ENTITY_TYPE, RavenDndOrigins.loc("powered_flight_forms"));

    private static final float FLIGHT_SPEED = 0.04F;
    private static final float STEEP_CLIMB_MULTIPLIER = 2.5F;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player != Minecraft.getInstance().player) return;
        if (!player.isFallFlying()) return;
        if (!isPoweredFlightForm(player.getId(), ShapeshiftClientState.getShapeshiftType(player.getId()))) return;

        Vec3 look = player.getLookAngle();
        Vec3 vel = player.getDeltaMovement();
        float speed = FLIGHT_SPEED * ShapeshiftClientState.flightSpeed(player.getId());

        if (player.getXRot() < -75 && player.getXRot() > -105) {
            speed *= STEEP_CLIMB_MULTIPLIER;
        }

        player.setDeltaMovement(vel.add(
                look.x * speed + (look.x * 1.5 - vel.x) * speed,
                look.y * speed + (look.y * 1.5 - vel.y) * speed,
                look.z * speed + (look.z * 1.5 - vel.z) * speed
        ));
    }

    public static boolean isPoweredFlightForm(int playerId, ResourceLocation shapeType) {
        if (shapeType == null) return false;
        boolean tagged = BuiltInRegistries.ENTITY_TYPE.getHolder(shapeType)
                .map(holder -> holder.is(POWERED_FLIGHT_FORMS))
                .orElse(false);
        return tagged || FakeEntityCache.getOrCreate(playerId, shapeType) instanceof FlyingAnimal;
    }
}
