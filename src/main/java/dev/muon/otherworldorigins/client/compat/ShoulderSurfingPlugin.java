package dev.muon.otherworldorigins.client.compat;

import com.github.exopandora.shouldersurfing.api.callback.IAdaptiveItemCallback;
import com.github.exopandora.shouldersurfing.api.callback.ITargetCameraOffsetCallback;
import com.github.exopandora.shouldersurfing.api.client.IShoulderSurfing;
import com.github.exopandora.shouldersurfing.api.plugin.IShoulderSurfingPlugin;
import com.github.exopandora.shouldersurfing.api.plugin.IShoulderSurfingRegistrar;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Plugin for Shoulder Surfing Reloaded to enable adaptive aiming during continuous spell casts.
 * This makes the player continuously aim at the crosshair target while casting continuous spells.
 */
public class ShoulderSurfingPlugin implements IShoulderSurfingPlugin {
    @Override
    public void register(IShoulderSurfingRegistrar registrar) {
        registrar.registerAdaptiveItemCallback(new ShouldAimAtTargetCallback());
        registrar.registerTargetCameraOffsetCallback(new WildshapeCameraOffsetCallback());
    }

    /**
     * Callback that returns true when the player is casting a continuous spell,
     * enabling the adaptive crosshair to make the player aim at the target.
     */
    private static class ShouldAimAtTargetCallback implements IAdaptiveItemCallback {
        @Override
        public boolean isHoldingAdaptiveItem(Minecraft minecraft, LivingEntity livingEntity) {
            return ShoulderSurfingIntegration.shouldAimAtTarget();
        }
    }

    /**
     * Adjusts shoulder-surfing offset after built-in modifiers: distance scales with wildshape height,
     * and extra downward vertical offset is applied for forms much shorter than the player (human eye
     * anchor vs. low model).
     */
    private static class WildshapeCameraOffsetCallback implements ITargetCameraOffsetCallback {
        @Override
        public Vec3 post(IShoulderSurfing instance, Vec3 targetOffset, Vec3 defaultOffset) {
            return ShoulderSurfingIntegration.scaleCameraOffsetForWildshape(targetOffset);
        }
    }
}
