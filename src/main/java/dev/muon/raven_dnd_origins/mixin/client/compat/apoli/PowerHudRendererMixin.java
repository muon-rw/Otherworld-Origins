package dev.muon.raven_dnd_origins.mixin.client.compat.apoli;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.muon.raven_dnd_origins.power.ActionOnAttackPower;
import dev.muon.raven_dnd_origins.power.ActionOnSpellDamagePower;
import dev.muon.raven_dnd_origins.power.DirectionalTeleportPower;
import dev.overgrown.apoli.client.PowerHudRenderer;
import dev.overgrown.apoli.data.HudRender;
import dev.overgrown.apoli.power.PowerType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Apoli resolves {@code hud_render} through a closed instanceof chain over its own power configs,
 * so third-party power types have to add their own configs here or their bars never draw.
 */
@Mixin(value = PowerHudRenderer.class, remap = false)
public class PowerHudRendererMixin {

    @ModifyReturnValue(method = "hudRenderOf", at = @At("RETURN"))
    private static @Nullable HudRender raven_dnd_origins$ownHudRender(@Nullable HudRender original,
                                                                      PowerType<?> type, Object config) {
        if (original != null) {
            return original;
        }
        if (config instanceof DirectionalTeleportPower.Configuration cfg) {
            return cfg.hudRender();
        }
        if (config instanceof ActionOnAttackPower.Configuration cfg) {
            return cfg.hudRender();
        }
        if (config instanceof ActionOnSpellDamagePower.Configuration cfg) {
            return cfg.hudRender();
        }
        return null;
    }
}
