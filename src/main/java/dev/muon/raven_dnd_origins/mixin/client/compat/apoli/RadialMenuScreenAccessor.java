package dev.muon.raven_dnd_origins.mixin.client.compat.apoli;

import dev.overgrown.apoli.client.radial.RadialMenu;
import dev.overgrown.apoli.client.radial.RadialMenuScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = RadialMenuScreen.class, remap = false)
public interface RadialMenuScreenAccessor {

    @Accessor("radialMenu")
    RadialMenu getRadialMenu();
}
