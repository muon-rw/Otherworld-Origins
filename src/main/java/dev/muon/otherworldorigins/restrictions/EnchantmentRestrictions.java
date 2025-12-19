package dev.muon.otherworldorigins.restrictions;

import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.config.OtherworldOriginsConfig;
import io.github.apace100.origins.registry.ModComponents;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.origin.OriginLayers;
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

        OriginComponent originComponent = ModComponents.ORIGIN.maybeGet(player).orElse(null);
        if (originComponent == null) {
            return true;
        }

        ResourceLocation classLayerLoc = OtherworldOrigins.loc("class");
        OriginLayer classLayer = OriginLayers.getLayer(classLayerLoc);
        if (classLayer == null) {
            return true;
        }

        Origin playerOrigin = originComponent.getOrigin(classLayer);
        if (playerOrigin == null || playerOrigin == Origin.EMPTY) {
            return true;
        }

        ResourceLocation requiredOriginLoc = OtherworldOrigins.loc("class/" + requiredClass);
        return playerOrigin.getIdentifier().equals(requiredOriginLoc);
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
