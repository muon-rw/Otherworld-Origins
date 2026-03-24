package dev.muon.otherworldorigins.client;

import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.client.shapeshift.ShapeshiftClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Provides powered elytra flight for the bald eagle wild shape.
 * When the player is fall-flying and shapeshifted as a bald eagle, continuous
 * thrust is applied in the look direction, preventing stalling and enabling
 * sustained flight without rockets.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = OtherworldOrigins.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EagleFlightHandler {

    private static final ResourceLocation BALD_EAGLE = ResourceLocation.fromNamespaceAndPath("alexsmobs", "bald_eagle");
    private static final float EAGLE_SPEED = 0.04F;
    private static final float STEEP_CLIMB_MULTIPLIER = 2.5F;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player != Minecraft.getInstance().player) return;
        if (!player.isFallFlying()) return;

        ResourceLocation shapeType = ShapeshiftClientState.getShapeshiftType(player.getId());
        if (!BALD_EAGLE.equals(shapeType)) return;

        Vec3 look = player.getLookAngle();
        Vec3 vel = player.getDeltaMovement();
        float speed = EAGLE_SPEED;

        if (player.getXRot() < -75 && player.getXRot() > -105) {
            speed *= STEEP_CLIMB_MULTIPLIER;
        }

        player.setDeltaMovement(vel.add(
                look.x * speed + (look.x * 1.5 - vel.x) * speed,
                look.y * speed + (look.y * 1.5 - vel.y) * speed,
                look.z * speed + (look.z * 1.5 - vel.z) * speed
        ));
    }
}
