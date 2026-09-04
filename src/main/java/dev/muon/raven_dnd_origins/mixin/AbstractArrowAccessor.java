package dev.muon.raven_dnd_origins.mixin;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractArrow.class)
public interface AbstractArrowAccessor {
    @Accessor("piercingIgnoreEntityIds")
    @Nullable
    IntOpenHashSet raven_dnd_origins$getPiercingIgnoreEntityIds();

    @Accessor("piercingIgnoreEntityIds")
    void raven_dnd_origins$setPiercingIgnoreEntityIds(@Nullable IntOpenHashSet set);
}
