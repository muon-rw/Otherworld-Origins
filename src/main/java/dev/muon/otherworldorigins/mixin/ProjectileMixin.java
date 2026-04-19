package dev.muon.otherworldorigins.mixin;

import dev.muon.otherworldorigins.power.ModifyProjectileAccuracyPower;
import dev.muon.otherworldorigins.power.ModifyProjectileVelocityPower;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Projectile.class)
public abstract class ProjectileMixin {

    @ModifyVariable(
            method = "shoot(DDDFF)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 1
    )
    private float otherworldorigins$modifyProjectileAccuracy(float inaccuracy) {
        Projectile self = (Projectile) (Object) this;
        if (!(self instanceof AbstractArrow)) {
            return inaccuracy;
        }
        Entity owner = self.getOwner();
        if (owner == null || !ModifyProjectileAccuracyPower.hasPower(owner)) {
            return inaccuracy;
        }
        return Math.max(0F, ModifyProjectileAccuracyPower.modify(owner, inaccuracy));
    }

    @ModifyArg(
            method = "shoot(DDDFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/Projectile;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"),
            index = 0
    )
    private Vec3 otherworldorigins$modifyProjectileVelocity(Vec3 vec3) {
        Projectile self = (Projectile) (Object) this;
        if (!(self instanceof AbstractArrow)) {
            return vec3;
        }
        Entity owner = self.getOwner();
        if (owner == null || !ModifyProjectileVelocityPower.hasPower(owner)) {
            return vec3;
        }
        double modifiedX = ModifyProjectileVelocityPower.modify(owner, (float) vec3.x);
        double modifiedY = ModifyProjectileVelocityPower.modify(owner, (float) vec3.y);
        double modifiedZ = ModifyProjectileVelocityPower.modify(owner, (float) vec3.z);
        return new Vec3(modifiedX, modifiedY, modifiedZ);
    }

    @Inject(method = "shoot(DDDFF)V", at = @At("RETURN"))
    private void otherworldorigins$executeProjectilePowerActions(double x, double y, double z, float velocity, float inaccuracy, CallbackInfo ci) {
        Projectile self = (Projectile) (Object) this;
        if (!(self instanceof AbstractArrow)) {
            return;
        }
        Entity owner = self.getOwner();
        if (owner == null) {
            return;
        }
        if (ModifyProjectileVelocityPower.hasPower(owner)) {
            ModifyProjectileVelocityPower.executeActions(owner);
        }
        if (ModifyProjectileAccuracyPower.hasPower(owner)) {
            ModifyProjectileAccuracyPower.executeActions(owner);
        }
    }
}
