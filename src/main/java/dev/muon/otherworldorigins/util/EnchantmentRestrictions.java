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
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
public class EnchantmentRestrictions {
    private static final Map<ResourceLocation, String> ENCHANTMENT_CLASS_MAP = new HashMap<>();

    static {
        ENCHANTMENT_CLASS_MAP.put(new ResourceLocation("minecraft:mending"), "artificer");
        ENCHANTMENT_CLASS_MAP.put(new ResourceLocation("minecraft:sweeping"), "barbarian");
        ENCHANTMENT_CLASS_MAP.put(new ResourceLocation("minecraft:luck_of_the_sea"), "bard");
        ENCHANTMENT_CLASS_MAP.put(new ResourceLocation("minecraft:fortune"), "cleric");
        ENCHANTMENT_CLASS_MAP.put(new ResourceLocation("minecraft:thorns"), "druid");
        ENCHANTMENT_CLASS_MAP.put(new ResourceLocation("minecraft:sharpness"), "fighter");
        ENCHANTMENT_CLASS_MAP.put(new ResourceLocation("minecraft:loyalty"), "monk");
        ENCHANTMENT_CLASS_MAP.put(new ResourceLocation("minecraft:smite"), "paladin");
        ENCHANTMENT_CLASS_MAP.put(new ResourceLocation("minecraft:infinity"), "ranger");
        ENCHANTMENT_CLASS_MAP.put(new ResourceLocation("apotheosis:endless_quiver"), "ranger");
        ENCHANTMENT_CLASS_MAP.put(new ResourceLocation("minecraft:power"), "rogue");
        ENCHANTMENT_CLASS_MAP.put(new ResourceLocation("minecraft:flame"), "warlock");
        ENCHANTMENT_CLASS_MAP.put(new ResourceLocation("minecraft:bane_of_arthropods"), "fighter");
    }

    public static boolean isEnchantmentAllowed(Player player, Enchantment enchantment) {
        ResourceLocation enchantmentId = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
        if (enchantmentId == null) {
            return true;
        }

        String requiredClass = ENCHANTMENT_CLASS_MAP.get(enchantmentId);
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

    public static String getRequiredClass(Enchantment enchantment) {
        ResourceLocation enchantmentId = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
        if (enchantmentId == null) return null;
        return ENCHANTMENT_CLASS_MAP.get(enchantmentId);
    }
}