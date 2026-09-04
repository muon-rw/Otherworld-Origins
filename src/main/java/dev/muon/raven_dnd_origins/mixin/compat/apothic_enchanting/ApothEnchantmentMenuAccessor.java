package dev.muon.raven_dnd_origins.mixin.compat.apothic_enchanting;

import dev.shadowsoffire.apothic_enchanting.table.ApothEnchantmentMenu;
import dev.shadowsoffire.apothic_enchanting.table.EnchantmentTableStats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ApothEnchantmentMenu.class, remap = false)
public interface ApothEnchantmentMenuAccessor {
    @Accessor("stats")
    EnchantmentTableStats raven_dnd_origins$getStats();
}
