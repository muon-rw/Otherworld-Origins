package dev.muon.otherworldorigins.mixin.client;

import dev.muon.otherworldorigins.power.PreventItemSlowdownPower;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Constant;

/**
 * {@link LocalPlayer#aiStep()} multiplies {@code input.leftImpulse} and {@code input.forwardImpulse}
 * by 0.2F whenever {@code isUsingItem() && !isPassenger()}. Returning 1.0F here skips the slowdown.
 * Targeting the constant (rather than the gating {@code isUsingItem()} call) means the power lookup
 * only runs when the slowdown branch actually executes — i.e. while using an item.
 * aiStep contains no other 0.2F literals, so {@code expect = 2} catches both assignments.
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerItemSlowdownMixin {

    @ModifyConstant(
            method = "aiStep",
            constant = @Constant(floatValue = 0.2F),
            expect = 2,
            require = 2
    )
    private float otherworldorigins$preventItemSlowdown(float original) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        return PreventItemSlowdownPower.has(self) ? 1.0F : original;
    }
}
