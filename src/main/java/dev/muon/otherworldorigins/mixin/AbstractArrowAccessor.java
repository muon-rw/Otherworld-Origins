package dev.muon.otherworldorigins.mixin;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractArrow.class)
public interface AbstractArrowAccessor {
    @Accessor("piercingIgnoreEntityIds")
    @Nullable
    IntOpenHashSet otherworldorigins$getPiercingIgnoreEntityIds();

    @Accessor("piercingIgnoreEntityIds")
    void otherworldorigins$setPiercingIgnoreEntityIds(@Nullable IntOpenHashSet set);
}
