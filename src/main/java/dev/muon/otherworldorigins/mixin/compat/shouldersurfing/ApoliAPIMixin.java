package dev.muon.otherworldorigins.mixin.compat.shouldersurfing;

import dev.muon.otherworldorigins.client.compat.ShoulderSurfingIntegration;
import dev.muon.otherworldorigins.config.OtherworldOriginsConfig;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

/**
 * Mixin to handle Shoulder Surfing compatibility for all active powers.
 * Before any active power is executed, we rotate the player to face the crosshair target
 * if they're in Shoulder Surfing mode. This ensures spells and other directional abilities
 * fire in the correct direction.
 */
@Mixin(value = ApoliAPI.class, remap = false)
public class ApoliAPIMixin {

    /**
     * Inject at the head of performPowers to rotate the player toward the crosshair
     * before the power activation packet is sent to the server.
     * 
     * Skips rotation if ALL powers being activated are in the rotation blacklist.
     */
    @Inject(method = "performPowers", at = @At("HEAD"))
    private static void onPerformPowers(Set<ResourceLocation> powers, CallbackInfo ci) {
        // Skip rotation if all powers are blacklisted
        if (OtherworldOriginsConfig.areAllPowersRotationBlacklisted(powers)) {
            return;
        }
        ShoulderSurfingIntegration.lookAtCrosshairTargetIfShoulderSurfing();
    }
}
