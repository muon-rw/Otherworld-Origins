package dev.muon.otherworldorigins.mixin.compat.irons_spellbooks.accessor;

import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = SyncedSpellData.class, remap = false)
public interface SyncedSpellDataAccessor {
    @Accessor("livingEntity")
    @Nullable
    LivingEntity otherworld$getLivingEntity();
}
