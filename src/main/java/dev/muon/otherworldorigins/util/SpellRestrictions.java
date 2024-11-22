package dev.muon.otherworldorigins.util;

import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.config.SpellCategoryConfig;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SpellRestrictions {
    private static Map<String, Set<SpellCategory>> SUBCLASS_SPELL_RESTRICTIONS = new HashMap<>();

    public static void initializeFromConfig() {
        Map<String, Set<SpellCategory>> newRestrictions = new HashMap<>();

        SpellCategoryConfig.getClassRestrictions().forEach((classPath, categories) -> {
            Set<SpellCategory> allowedCategories = categories.stream()
                    .map(cat -> SpellCategory.valueOf(cat.toUpperCase()))
                    .collect(Collectors.toSet());
            newRestrictions.put(classPath, allowedCategories);
        });

        SUBCLASS_SPELL_RESTRICTIONS = newRestrictions;
    }


    private static final Map<UUID, PlayerClassInfo> playerClassCache = new ConcurrentHashMap<>();

    private static class PlayerClassInfo {
        String className;
        String subclassName;
        long lastUpdateTime;

        PlayerClassInfo(String className, String subclassName) {
            this.className = className;
            this.subclassName = subclassName;
            this.lastUpdateTime = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - lastUpdateTime > 10000;
        }
    }

    private static PlayerClassInfo getPlayerClassInfo(@NotNull Player player) {
        UUID playerUUID = player.getUUID();
        PlayerClassInfo cachedInfo = playerClassCache.get(playerUUID);

        if (cachedInfo != null && !cachedInfo.isExpired()) {
            return cachedInfo;
        }

        return IOriginContainer.get(player).resolve().map(container -> {
            Registry<OriginLayer> layerRegistry = OriginsAPI.getLayersRegistry(player.level().getServer());
            ResourceLocation classLayerLoc = OtherworldOrigins.loc("class");
            ResourceLocation subclassLayerLoc = OtherworldOrigins.loc("subclass");

            ResourceKey<OriginLayer> classLayerKey = ResourceKey.create(layerRegistry.key(), classLayerLoc);
            ResourceKey<OriginLayer> subclassLayerKey = ResourceKey.create(layerRegistry.key(), subclassLayerLoc);

            Holder<OriginLayer> classLayerHolder = layerRegistry.getHolder(classLayerKey).orElse(null);
            Holder<OriginLayer> subclassLayerHolder = layerRegistry.getHolder(subclassLayerKey).orElse(null);

            if (classLayerHolder == null || subclassLayerHolder == null) {
                OtherworldOrigins.LOGGER.warn("Class or subclass layer not found");
                return null;
            }

            ResourceKey<Origin> playerClassKey = container.getOrigin(classLayerHolder);
            ResourceKey<Origin> playerSubclassKey = container.getOrigin(subclassLayerHolder);

            if (playerClassKey == null || playerSubclassKey == null) {
                OtherworldOrigins.LOGGER.warn("Player class or subclass not found");
                return null;
            }

            String className = playerClassKey.location().getPath().replace("class/", "");
            String subclassName = playerSubclassKey.location().getPath().substring(playerSubclassKey.location().getPath().lastIndexOf('/') + 1);

            PlayerClassInfo newInfo = new PlayerClassInfo(className, subclassName);
            playerClassCache.put(playerUUID, newInfo);
            return newInfo;
        }).orElse(null);
    }

    public static boolean isSpellAllowed(Player player, AbstractSpell spell) {
        if (SpellCategoryConfig.isSpellUnrestricted(spell)) {
            return true;
        }
        PlayerClassInfo classInfo = getPlayerClassInfo(player);
        if (classInfo == null) {
            return true;
        }

        String fullSubclassKey = classInfo.className + "/" + classInfo.subclassName;
        Set<SpellCategory> subclassRestrictions = SUBCLASS_SPELL_RESTRICTIONS.get(fullSubclassKey);
        if (subclassRestrictions != null) {
            if (subclassRestrictions.isEmpty()) {
                return false;
            }
            SpellCategory category = SpellCategoryMapper.getCategory(spell);
            return subclassRestrictions.contains(category);
        }

        return true;
    }

    public static Component getRestrictionMessage(Player player, AbstractSpell spell) {
        PlayerClassInfo classInfo = getPlayerClassInfo(player);
        if (classInfo == null) {
            return Component.literal("Unable to determine class restrictions")
                    .withStyle(ChatFormatting.DARK_GRAY);
        }

        String fullSubclassKey = classInfo.className + "/" + classInfo.subclassName;
        Set<SpellCategory> allowedCategories = SUBCLASS_SPELL_RESTRICTIONS.get(fullSubclassKey);

        String formattedSubclass = Arrays.stream(classInfo.subclassName.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));

        String formattedClass = classInfo.className.substring(0, 1).toUpperCase() +
                classInfo.className.substring(1).toLowerCase() + "s";

        if (allowedCategories == null || allowedCategories.isEmpty()) {
            return Component.literal(String.format("%s %s cannot cast any spells",
                            formattedSubclass, formattedClass))
                    .withStyle(ChatFormatting.DARK_GRAY);
        }

        if (allowedCategories.size() == 1) {
            String category = allowedCategories.iterator().next().toString().toLowerCase();
            return Component.literal(String.format("%s %s can only cast %s spells",
                            formattedSubclass, formattedClass, category))
                    .withStyle(ChatFormatting.DARK_GRAY);
        }

        // TODO: Formatting in case we want to have more than 2 categories
        String categories = allowedCategories.stream()
                .map(cat -> cat.toString().toLowerCase())
                .collect(Collectors.joining(" and "));

        return Component.literal(String.format("%s %s can only cast %s spells",
                        formattedSubclass, formattedClass, categories))
                .withStyle(ChatFormatting.DARK_GRAY);
    }

    public static void clearCache(Player player) {
        playerClassCache.remove(player.getUUID());
    }
}