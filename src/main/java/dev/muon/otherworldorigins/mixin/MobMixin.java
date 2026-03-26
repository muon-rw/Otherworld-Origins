package dev.muon.otherworldorigins.mixin;

import dev.muon.otherworldorigins.power.MobsIgnorePower;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobMixin {

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void otherworld$preventTargeting(LivingEntity target, CallbackInfo ci) {
        if (target instanceof Player player) {
            Mob self = (Mob) (Object) this;
            if (MobsIgnorePower.preventsMobFromTargeting(self, player)) {
                ci.cancel();
            }
        }
    }
}
