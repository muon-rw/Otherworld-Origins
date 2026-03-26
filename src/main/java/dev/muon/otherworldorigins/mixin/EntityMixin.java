package dev.muon.otherworldorigins.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.muon.otherworldorigins.power.ModifyMaxAirSupplyPower;
import dev.muon.otherworldorigins.power.PreventBlockSlowdownPower;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
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
}
