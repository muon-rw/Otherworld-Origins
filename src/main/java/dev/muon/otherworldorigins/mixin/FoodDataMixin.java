package dev.muon.otherworldorigins.mixin;

import dev.muon.otherworldorigins.power.HungerImmunityPower;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public abstract class FoodDataMixin {

    @Shadow
    private int tickTimer;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void otherworldorigins$hungerImmunityTick(Player player, CallbackInfo ci) {
        if (HungerImmunityPower.has(player)) {
            FoodData self = (FoodData) (Object) this;
            self.setFoodLevel(20);
            self.setSaturation(5.0F);
            self.setExhaustion(0.0F);
            this.tickTimer = 0;
            ci.cancel();
        }
    }
}
