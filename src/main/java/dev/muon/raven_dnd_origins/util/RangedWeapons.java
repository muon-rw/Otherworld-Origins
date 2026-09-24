package dev.muon.raven_dnd_origins.util;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class RangedWeapons {
    private static final ResourceLocation RANGED_WEAPON_PROPERTIES = ResourceLocation.fromNamespaceAndPath("ranged_weapon", "properties");

    private RangedWeapons() {}

    public static boolean hasRangedWeaponProperties(ItemStack stack) {
        DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(RANGED_WEAPON_PROPERTIES);
        return type != null && stack.has(type);
    }
}
