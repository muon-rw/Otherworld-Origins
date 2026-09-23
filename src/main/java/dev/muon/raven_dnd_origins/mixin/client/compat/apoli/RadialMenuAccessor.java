package dev.muon.raven_dnd_origins.mixin.client.compat.apoli;

import dev.overgrown.apoli.client.radial.RadialMenu;
import dev.overgrown.apoli.network.payload.RadialMenuOpenS2C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(value = RadialMenu.class, remap = false)
public interface RadialMenuAccessor {

    @Accessor("entries")
    List<RadialMenuOpenS2C.Entry> getEntries();
}
