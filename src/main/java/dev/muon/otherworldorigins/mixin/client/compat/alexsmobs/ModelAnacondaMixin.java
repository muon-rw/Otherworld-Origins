package dev.muon.otherworldorigins.mixin.client.compat.alexsmobs;

import com.github.alexthe666.alexsmobs.client.model.ModelAnaconda;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Alex's Mobs applies head pitch with {@code Math.min(0, pitchRad)}, so only "looking up"
 * (negative {@link net.minecraft.world.entity.Entity#getXRot()}) affects the head; looking
 * down / moving downward never tilts the model. Use the full pitch for shapeshift vertical
 * motion and normal look pitch.
 */
@Mixin(value = ModelAnaconda.class, remap = false)
public class ModelAnacondaMixin {

    @WrapOperation(
            method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/Math;min(FF)F"
            )
    )
    private float otherworldorigins$fullHeadPitchRadians(float a, float b, Operation<Float> original,
                                                          LivingEntity entity,
                                                          float limbSwing,
                                                          float limbSwingAmount,
                                                          float ageInTicks,
                                                          float netHeadYaw,
                                                          float headPitch) {
        return b;
    }
}
