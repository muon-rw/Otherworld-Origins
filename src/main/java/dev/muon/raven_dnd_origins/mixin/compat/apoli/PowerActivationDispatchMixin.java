package dev.muon.raven_dnd_origins.mixin.compat.apoli;

import dev.muon.raven_dnd_origins.power.DirectionalTeleportPower;
import dev.overgrown.apoli.Apoli;
import dev.overgrown.apoli.ApoliNetwork;
import dev.overgrown.apoli.network.payload.PowerActivatedS2C;
import dev.overgrown.apoli.network.payload.PowerActivationC2S;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerResources;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A player's keybound power activation arrives as {@link PowerActivationC2S} and is routed by a closed
 * instanceof chain over Apoli's own active power types, so a third-party one has to be run here.
 * TAIL is reached only once Apoli's grab, ownership, suppression and condition checks have all passed.
 */
@Mixin(value = Apoli.class, remap = false)
public class PowerActivationDispatchMixin {

    @Inject(method = "handleActivation", at = @At("TAIL"))
    private static void raven_dnd_origins$activateOwnKeyboundPowers(
            ServerPlayer player, PowerActivationC2S payload, CallbackInfo ci) {
        Power loaded = ApoliPowers.get(payload.power());
        if (loaded == null || !(loaded.config() instanceof DirectionalTeleportPower.Configuration cfg)) {
            return;
        }
        if (DirectionalTeleportPower.onKeyPressed(player, cfg.key().key()) <= 0) {
            return;
        }
        PowerContainer container = PowerContainer.of(player);
        if (container == null) {
            return;
        }
        int cooldown = PowerResources.readDeadline(container, payload.power()).orElse(0);
        if (cooldown > 0) {
            ApoliNetwork.sendActivated(player, new PowerActivatedS2C(payload.power(), cooldown));
        }
    }
}
