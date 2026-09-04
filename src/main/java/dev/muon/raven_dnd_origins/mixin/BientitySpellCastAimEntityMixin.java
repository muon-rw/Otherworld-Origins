package dev.muon.raven_dnd_origins.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.muon.raven_dnd_origins.util.spell.BientitySpellCastAim;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Server-only synthetic facing for {@link BientitySpellCastAim} during bi-entity {@code cast_spell}.
 * Each injector takes the fast-skip null path when no cast is active, keeping the per-tick rotation getters
 * near zero cost.
 */
@Mixin(Entity.class)
public class BientitySpellCastAimEntityMixin {

    @ModifyReturnValue(method = "getLookAngle", at = @At("RETURN"))
    private Vec3 raven_dnd_origins$bientityLookAngle(Vec3 original) {
        BientitySpellCastAim.Frame frame = BientitySpellCastAim.activeFrameOrNull((Entity) (Object) this);
        return frame != null ? frame.lookDirection() : original;
    }

    @ModifyReturnValue(method = "getForward", at = @At("RETURN"))
    private Vec3 raven_dnd_origins$bientityForward(Vec3 original) {
        BientitySpellCastAim.Frame frame = BientitySpellCastAim.activeFrameOrNull((Entity) (Object) this);
        return frame != null ? frame.lookDirection() : original;
    }

    @ModifyReturnValue(method = "getXRot", at = @At("RETURN"))
    private float raven_dnd_origins$bientityXRot(float original) {
        BientitySpellCastAim.Frame frame = BientitySpellCastAim.activeFrameOrNull((Entity) (Object) this);
        return frame != null ? frame.xRot() : original;
    }

    @ModifyReturnValue(method = "getYRot", at = @At("RETURN"))
    private float raven_dnd_origins$bientityYRot(float original) {
        BientitySpellCastAim.Frame frame = BientitySpellCastAim.activeFrameOrNull((Entity) (Object) this);
        return frame != null ? frame.yRot() : original;
    }

    @ModifyReturnValue(method = "getRotationVector", at = @At("RETURN"))
    private Vec2 raven_dnd_origins$bientityRotationVector(Vec2 original) {
        BientitySpellCastAim.Frame frame = BientitySpellCastAim.activeFrameOrNull((Entity) (Object) this);
        return frame != null ? frame.rotationVector() : original;
    }

    @ModifyReturnValue(method = "getViewVector", at = @At("RETURN"))
    private Vec3 raven_dnd_origins$bientityViewVector(Vec3 original) {
        BientitySpellCastAim.Frame frame = BientitySpellCastAim.activeFrameOrNull((Entity) (Object) this);
        return frame != null ? frame.lookDirection() : original;
    }

    @ModifyReturnValue(method = "getViewXRot", at = @At("RETURN"))
    private float raven_dnd_origins$bientityViewXRot(float original) {
        BientitySpellCastAim.Frame frame = BientitySpellCastAim.activeFrameOrNull((Entity) (Object) this);
        return frame != null ? frame.xRot() : original;
    }

    @ModifyReturnValue(method = "getViewYRot", at = @At("RETURN"))
    private float raven_dnd_origins$bientityViewYRot(float original) {
        BientitySpellCastAim.Frame frame = BientitySpellCastAim.activeFrameOrNull((Entity) (Object) this);
        return frame != null ? frame.yRot() : original;
    }
}
