package dev.muon.otherworldorigins.mixin.client;

import net.minecraft.world.entity.WalkAnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WalkAnimationState.class)
public interface WalkAnimationStateAccessor {
    @Accessor("speedOld")
    float getSpeedOld();

    @Accessor("speedOld")
    void setSpeedOld(float value);

    @Accessor("position")
    void setPosition(float value);
}
