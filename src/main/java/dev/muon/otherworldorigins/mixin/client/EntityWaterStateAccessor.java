package dev.muon.otherworldorigins.mixin.client;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityWaterStateAccessor {
    @Accessor("wasTouchingWater")
    void setWasTouchingWater(boolean value);
}
