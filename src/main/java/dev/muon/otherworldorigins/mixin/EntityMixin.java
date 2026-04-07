package dev.muon.otherworldorigins.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.muon.otherworldorigins.power.ModifyMaxAirSupplyPower;
import dev.muon.otherworldorigins.power.PreventBlockSlowdownPower;
import dev.muon.otherworldorigins.power.ShapeshiftPower;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {

    @ModifyReturnValue(method = "getMaxAirSupply", at = @At("RETURN"))
    private int otherworldorigins$modifyMaxAirSupply(int original) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof Player player)) {
            return original;
        }
        return original + ModifyMaxAirSupplyPower.getTotalAirBonus(player);
    }

    @Inject(method = "makeStuckInBlock", at = @At("HEAD"), cancellable = true)
    private void otherworld$preventBlockSlowdown(BlockState state, Vec3 motionMultiplier, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (PreventBlockSlowdownPower.shouldPreventSlowdown(self, state)) {
            ci.cancel();
        }
    }

    /**
     * {@link net.minecraft.world.entity.Entity#updateSwimming()} clears swimming unless sprinting or
     * (when not already swimming) underwater / {@code canStartSwimming}. For aquatic wildshape we
     * satisfy those checks without requiring sprint, and we allow starting swim from fluid contact
     * even when {@code canStartSwimming} would be false (e.g. eyes above the surface).
     */
    @WrapOperation(
            method = "updateSwimming",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isSprinting()Z")) // intentionally both calls
    private boolean otherworldorigins$wrapIsSprintingWhileSwimming(Entity instance, Operation<Boolean> original) {
        return this.otherworldorigins$shouldForceAquaticWildshapeSwim() || original.call(instance);
    }

    @WrapOperation(
            method = "updateSwimming",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;canStartSwimming()Z", remap = false)) // canStartSwimming is interface patched by Forge
    private boolean otherworldorigins$wrapCanStartSwimming(Entity instance, Operation<Boolean> original) {
        return this.otherworldorigins$shouldForceAquaticWildshapeSwim() || original.call(instance);
    }

    @Unique
    private boolean otherworldorigins$shouldForceAquaticWildshapeSwim() {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof Player player)) {
            return false;
        }
        ShapeshiftPower.Configuration config = ShapeshiftPower.getActiveShapeshiftConfig(player);
        return config != null && config.autoSwimInWater();
    }

}
