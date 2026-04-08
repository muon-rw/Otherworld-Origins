package dev.muon.otherworldorigins.mixin.origins_patches;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.apoli.api.power.factory.power.CooldownPowerFactory;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * The Forge port's {@link CooldownPowerFactory#onGained} sets lastUseTime to the current game time,
 * putting every cooldown power on full recharge when first gained. Upstream Fabric has no such
 * override — lastUseTime defaults to 0, so all cooldown powers start immediately usable.
 * This mixin restores upstream behavior by nooping onGained.
 */
@Mixin(value = CooldownPowerFactory.class, remap = false)
public class CooldownPowerFactoryActiveSelfMixin {

    @WrapMethod(method = "onGained")
    private void otherworld$skipInitialCooldown(
            ConfiguredPower<?, ?> configuration,
            Entity entity,
            Operation<Void> original
    ) {
    }
}
