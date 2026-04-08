package dev.muon.otherworldorigins.mixin.origins_patches;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.apoli.api.power.configuration.power.ICooldownPowerConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.power.CooldownPowerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Forge Apoli's {@link CooldownPowerFactory#onGained} sets {@code lastUseTime} to the current game time, as if the power
 * were just used. Upstream Fabric initializes {@code lastUseTime} to {@code 0}, which is <em>not</em> the same as
 * "ready": remaining ticks are {@code duration - worldGameTime}, so young worlds still show a nearly full bar.
 * <p>
 * Matching {@link CooldownPowerFactory#assign}'s convention for zero remaining cooldown:
 * {@code lastUseTime = gameTime - duration + 0}, so the ability is actually usable as soon as it is gained,
 * independent of world age.
 * <p>
 * Scoped with {@link ModifyArg} to the {@code setLastUseTime} call inside {@code onGained} only — {@code use()} must keep
 * real {@code gameTime}.
 */
@Mixin(value = CooldownPowerFactory.class, remap = false)
public class CooldownPowerFactoryActiveSelfMixin {

    @ModifyArg(
            method = "onGained(Lio/github/edwinmindcraft/apoli/api/power/configuration/ConfiguredPower;Lnet/minecraft/world/entity/Entity;)V",
            at = @At(value = "INVOKE", target = "Lio/github/edwinmindcraft/apoli/api/power/factory/power/CooldownPowerFactory;setLastUseTime(Lio/github/edwinmindcraft/apoli/api/power/configuration/ConfiguredPower;Lnet/minecraft/world/entity/Entity;J)V"),
            index = 2,
            remap = false
    )
    private long otherworld$lastUseTimeForZeroRemainingOnGain(
            long gameTime,
            @Local(argsOnly = true) ConfiguredPower<?, ?> configuration
    ) {
        int duration = ((ICooldownPowerConfiguration) configuration.getConfiguration()).duration();
        return gameTime - duration;
    }
}
