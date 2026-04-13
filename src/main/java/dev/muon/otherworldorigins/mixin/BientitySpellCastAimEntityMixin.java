package dev.muon.otherworldorigins.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.muon.otherworldorigins.util.spell.BientitySpellCastAim;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Server-only synthetic facing for {@link BientitySpellCastAim} during bi-entity {@code cast_spell}.
 */
@Mixin(Entity.class)
public class BientitySpellCastAimEntityMixin {

    @ModifyReturnValue(method = "getLookAngle", at = @At("RETURN"))
    private Vec3 otherworldorigins$bientityLookAngle(Vec3 original) {
        Entity self = (Entity) (Object) this;
        return BientitySpellCastAim.activeFrameFor(self)
                .map(BientitySpellCastAim.Frame::lookDirection)
                .orElse(original);
    }

    @ModifyReturnValue(method = "getForward", at = @At("RETURN"))
    private Vec3 otherworldorigins$bientityForward(Vec3 original) {
        Entity self = (Entity) (Object) this;
        return BientitySpellCastAim.activeFrameFor(self)
                .map(BientitySpellCastAim.Frame::lookDirection)
                .orElse(original);
    }

    @ModifyReturnValue(method = "getXRot", at = @At("RETURN"))
    private float otherworldorigins$bientityXRot(float original) {
        Entity self = (Entity) (Object) this;
        return BientitySpellCastAim.activeFrameFor(self)
                .map(BientitySpellCastAim.Frame::xRot)
                .orElse(original);
    }

    @ModifyReturnValue(method = "getYRot", at = @At("RETURN"))
    private float otherworldorigins$bientityYRot(float original) {
        Entity self = (Entity) (Object) this;
        return BientitySpellCastAim.activeFrameFor(self)
                .map(BientitySpellCastAim.Frame::yRot)
                .orElse(original);
    }

    @ModifyReturnValue(method = "getRotationVector", at = @At("RETURN"))
    private Vec2 otherworldorigins$bientityRotationVector(Vec2 original) {
        Entity self = (Entity) (Object) this;
        return BientitySpellCastAim.activeFrameFor(self)
                .map(f -> new Vec2(f.xRot(), f.yRot()))
                .orElse(original);
    }

    @ModifyReturnValue(method = "getViewVector", at = @At("RETURN"))
    private Vec3 otherworldorigins$bientityViewVector(Vec3 original) {
        Entity self = (Entity) (Object) this;
        return BientitySpellCastAim.activeFrameFor(self)
                .map(BientitySpellCastAim.Frame::lookDirection)
                .orElse(original);
    }

    @ModifyReturnValue(method = "getViewXRot", at = @At("RETURN"))
    private float otherworldorigins$bientityViewXRot(float original) {
        Entity self = (Entity) (Object) this;
        return BientitySpellCastAim.activeFrameFor(self)
                .map(BientitySpellCastAim.Frame::xRot)
                .orElse(original);
    }

    @ModifyReturnValue(method = "getViewYRot", at = @At("RETURN"))
    private float otherworldorigins$bientityViewYRot(float original) {
        Entity self = (Entity) (Object) this;
        return BientitySpellCastAim.activeFrameFor(self)
                .map(BientitySpellCastAim.Frame::yRot)
                .orElse(original);
    }
}
