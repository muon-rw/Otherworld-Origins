package dev.muon.otherworldorigins.client.compat;

import com.github.exopandora.shouldersurfing.api.callback.IAdaptiveItemCallback;
import com.github.exopandora.shouldersurfing.api.plugin.IShoulderSurfingPlugin;
import com.github.exopandora.shouldersurfing.api.plugin.IShoulderSurfingRegistrar;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

/**
 * Plugin for Shoulder Surfing Reloaded to enable adaptive aiming during continuous spell casts.
 * This makes the player continuously aim at the crosshair target while casting continuous spells.
 */
public class ShoulderSurfingPlugin implements IShoulderSurfingPlugin {
    @Override
    public void register(IShoulderSurfingRegistrar registrar) {
        // Register callback to enable adaptive crosshair during continuous spell casts
        registrar.registerAdaptiveItemCallback(new ShouldAimAtTargetCallback());
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
}
