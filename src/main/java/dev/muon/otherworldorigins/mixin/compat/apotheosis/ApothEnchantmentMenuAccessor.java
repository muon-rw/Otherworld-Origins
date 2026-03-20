package dev.muon.otherworldorigins.mixin.compat.apotheosis;

import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ApothEnchantmentMenu.class, remap = false)
public interface ApothEnchantmentMenuAccessor {
    @Accessor("stats")
    ApothEnchantmentMenu.TableStats otherworldorigins$getStats();
}
