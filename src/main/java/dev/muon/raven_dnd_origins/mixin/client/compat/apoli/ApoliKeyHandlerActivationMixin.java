package dev.muon.raven_dnd_origins.mixin.client.compat.apoli;

import dev.muon.raven_dnd_origins.power.DirectionalTeleportPower;
import dev.overgrown.apoli.client.ApoliKeyHandler;
import dev.overgrown.apoli.client.ApoliKeyMappings;
import dev.overgrown.apoli.client.ClientPowerState;
import dev.overgrown.apoli.network.payload.PowerActivationC2S;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Apoli only polls keys for its own active power types, so a third-party one is never offered the
 * press and no activation packet is ever sent. TAIL runs after Apoli's own per-tick key bookkeeping.
 */
@Mixin(value = ApoliKeyHandler.class, remap = false)
public class ApoliKeyHandlerActivationMixin {

    @Inject(method = "onClientTick", at = @At("TAIL"))
    private static void raven_dnd_origins$sendOwnKeyboundActivations(CallbackInfo ci) {
        for (ResourceLocation powerId : ClientPowerState.localPowers()) {
            Power power = ApoliPowers.get(powerId);
            if (power == null || !(power.config() instanceof DirectionalTeleportPower.Configuration cfg)) {
                continue;
            }
            KeyMapping mapping = ApoliKeyMappings.resolve(cfg.key().key());
            if (mapping == null || !ApoliKeyMappings.consumePress(mapping, cfg.key().continuous())) {
                continue;
            }
            if (ClientPowerState.getCooldown(powerId) > 0) {
                continue;
            }
            PacketDistributor.sendToServer(new PowerActivationC2S(powerId));
        }
    }
}
