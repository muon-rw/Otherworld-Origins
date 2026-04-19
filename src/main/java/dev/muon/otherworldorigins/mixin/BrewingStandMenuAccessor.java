package dev.muon.otherworldorigins.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.BrewingStandMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BrewingStandMenu.class)
public interface BrewingStandMenuAccessor {
    @Accessor("brewingStand")
    Container otherworld$brewingStand();
}
