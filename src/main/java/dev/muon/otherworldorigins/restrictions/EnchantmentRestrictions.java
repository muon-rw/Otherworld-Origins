package dev.muon.otherworldorigins.restrictions;

import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.config.OtherworldOriginsConfig;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class EnchantmentRestrictions {
    private static final Map<ResourceLocation, String> ENCHANTMENT_CLASS_MAP = new HashMap<>();

    static {
        ENCHANTMENT_CLASS_MAP.put(new ResourceLocation("minecraft:mending"), "artificer");
        ENCHANTMENT_CLASS_MAP.put(new ResourceLocation("backpacked:repairman"), "artificer");
        ENCHANTMENT_CLASS_MAP.put(new ResourceLocation("minecraft:sweeping"), "barbarian");
        ENCHANTMENT_CLASS_MAP.put(new ResourceLocation("minecraft:thorns"), "druid");
        ENCHANTMENT_CLASS_MAP.put(new ResourceLocation("minecraft:sharpness"), "fighter");
        ENCHANTMENT_CLASS_MAP.put(new ResourceLocation("minecraft:feather_falling"), "monk");
        ENCHANTMENT_CLASS_MAP.put(new ResourceLocation("minecraft:smite"), "paladin");
        ENCHANTMENT_CLASS_MAP.put(new ResourceLocation("minecraft:power"), "rogue");
        ENCHANTMENT_CLASS_MAP.put(new ResourceLocation("minecraft:infinity"), "ranger");
        ENCHANTMENT_CLASS_MAP.put(new ResourceLocation("apotheosis:endless_quiver"), "ranger");
    }

    public static boolean isEnchantmentAllowed(Player player, Enchantment enchantment) {
        if (!OtherworldOriginsConfig.ENABLE_ENCHANTMENT_RESTRICTIONS.get()) {
            return true;
        }
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

    public static List<Enchantment> getEnchantmentTextForClass(String className) {
        return ENCHANTMENT_CLASS_MAP.entrySet().stream()
                .filter(entry -> entry.getValue().equals(className))
                .filter(entry -> entry.getKey().getNamespace().equals("minecraft"))   // Only minecraft enchantments, because this is just for text
                .map(entry -> ForgeRegistries.ENCHANTMENTS.getValue(entry.getKey()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

}