package dev.muon.otherworldorigins.util;

import dev.muon.otherworldorigins.OtherworldOrigins;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.HashMap;
import java.util.Map;

public class EnchantmentRestrictions {
    private static final Map<Enchantment, String> ENCHANTMENT_CLASS_MAP = new HashMap<>();

    static {
        ENCHANTMENT_CLASS_MAP.put(Enchantments.MENDING, "artificer");
        ENCHANTMENT_CLASS_MAP.put(Enchantments.SWEEPING_EDGE, "barbarian");
        ENCHANTMENT_CLASS_MAP.put(Enchantments.FISHING_LUCK, "bard");
        ENCHANTMENT_CLASS_MAP.put(Enchantments.BLOCK_FORTUNE, "cleric");
        ENCHANTMENT_CLASS_MAP.put(Enchantments.THORNS, "druid");
        ENCHANTMENT_CLASS_MAP.put(Enchantments.SHARPNESS, "fighter");
        ENCHANTMENT_CLASS_MAP.put(Enchantments.LOYALTY, "monk");
        ENCHANTMENT_CLASS_MAP.put(Enchantments.SMITE, "paladin");
        ENCHANTMENT_CLASS_MAP.put(Enchantments.INFINITY_ARROWS, "ranger");
        ENCHANTMENT_CLASS_MAP.put(Enchantments.POWER_ARROWS, "rogue");
        ENCHANTMENT_CLASS_MAP.put(Enchantments.FIRE_ASPECT, "warlock");
    }

    public static boolean isEnchantmentAllowed(Player player, Enchantment enchantment) {
        String requiredClass = ENCHANTMENT_CLASS_MAP.get(enchantment);
        if (requiredClass == null) {
            return true;
        }

        return IOriginContainer.get(player).resolve().map(container -> {
            ResourceLocation classLayerLoc = OtherworldOrigins.loc("class");
            OriginLayer classLayer = OriginsAPI.getLayersRegistry().get(classLayerLoc);
            if (classLayer == null) {
                return true;
            }

            ResourceKey<Origin> playerOrigin = container.getOrigin(ResourceKey.create(OriginsAPI.getLayersRegistry().key(), classLayerLoc));
            ResourceLocation requiredOriginLoc = OtherworldOrigins.loc("class/" + requiredClass);
            return playerOrigin.location().equals(requiredOriginLoc);
        }).orElse(true);
    }
}