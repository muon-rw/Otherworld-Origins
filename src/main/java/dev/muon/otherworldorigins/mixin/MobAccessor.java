package dev.muon.otherworldorigins.mixin;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.Nullable;

@Mixin(Mob.class)
public interface MobAccessor {
    @Invoker("getAmbientSound")
    @Nullable
    SoundEvent invokeGetAmbientSound();
}
