package dev.muon.otherworldorigins.mixin;

import dev.muon.otherworldorigins.power.ModifyPierceLevelPower;
import dev.muon.otherworldorigins.power.MomentumPower;
import dev.muon.otherworldorigins.power.MultishotPower;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin {

    @Inject(method = "shoot(DDDFF)V", at = @At("RETURN"))
    private void otherworldorigins$modifyPierceLevel(double x, double y, double z, float velocity, float inaccuracy, CallbackInfo ci) {
        AbstractArrow arrow = (AbstractArrow) (Object) this;
        Entity owner = arrow.getOwner();
        if (owner == null) {
            return;
        }
        if (ModifyPierceLevelPower.hasPower(owner)) {
            float modifiedPierce = ModifyPierceLevelPower.modify(owner, arrow.getPierceLevel());
            arrow.setPierceLevel((byte) Math.max(0, Math.round(modifiedPierce)));
            ModifyPierceLevelPower.executeActions(owner);
        }
        MultishotPower.spawnExtras(arrow, owner);
        MomentumPower.applyRecoil(arrow, owner);
    }
}
