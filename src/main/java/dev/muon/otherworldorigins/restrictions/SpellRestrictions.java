package dev.muon.otherworldorigins.restrictions;

import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.config.OtherworldOriginsConfig;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SpellRestrictions {
    private static Map<String, Set<SpellCategory>> SUBCLASS_SPELL_RESTRICTIONS = new HashMap<>();
    private static Map<String, Set<ResourceLocation>> SUBCLASS_SCHOOL_RESTRICTIONS = new HashMap<>();

    public static void initializeFromConfig() {
        Map<String, Set<SpellCategory>> newRestrictions = new HashMap<>();
        Map<String, Set<ResourceLocation>> newSchoolRestrictions = new HashMap<>();

        OtherworldOriginsConfig.getClassRestrictions().forEach((classPath, categories) -> {
            Set<SpellCategory> allowedCategories = categories.stream()
                    .map(cat -> SpellCategory.valueOf(cat.toUpperCase()))
                    .collect(Collectors.toSet());
            newRestrictions.put(classPath, allowedCategories);
        });

        OtherworldOriginsConfig.getSchoolRestrictions().forEach((classPath, schools) -> {
            Set<ResourceLocation> allowedSchools = schools.stream()
                    .map(school -> {
                        if (!school.contains(":")) {
                            return new ResourceLocation("irons_spellbooks", school);
                        }
                        return new ResourceLocation(school);
                    })
                    .collect(Collectors.toSet());
            newSchoolRestrictions.put(classPath, allowedSchools);
        });

        SUBCLASS_SPELL_RESTRICTIONS = newRestrictions;
        SUBCLASS_SCHOOL_RESTRICTIONS = newSchoolRestrictions;
    }
    
    public static Set<ResourceLocation> getAllowedSchools(String subclassKey) {
        return SUBCLASS_SCHOOL_RESTRICTIONS.getOrDefault(subclassKey, Collections.emptySet());
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
        if (OtherworldOriginsConfig.isSpellUnrestricted(spell)) {
            return true;
        }
        PlayerClassInfo classInfo = getPlayerClassInfo(player);
        if (classInfo == null) {
            return true;
        }

        String fullSubclassKey = classInfo.className + "/" + classInfo.subclassName;
        Set<SpellCategory> subclassRestrictions = SUBCLASS_SPELL_RESTRICTIONS.get(fullSubclassKey);
        Set<ResourceLocation> schoolRestrictions = SUBCLASS_SCHOOL_RESTRICTIONS.get(fullSubclassKey);
        
        // If has all 3 categories, spell is always allowed
        if (subclassRestrictions != null && subclassRestrictions.size() == 3) {
            return true;
        }
        
        // Check category restrictions - spell is allowed if ANY of its categories match
        if (subclassRestrictions != null && !subclassRestrictions.isEmpty()) {
            Set<SpellCategory> spellCategories = SpellCategoryMapper.getCategories(spell);
            // Check if any of the spell's categories are in the allowed set
            for (SpellCategory category : spellCategories) {
                if (subclassRestrictions.contains(category)) {
                    return true;
                }
            }
        }
        
        // Check school restrictions (additive to category)
        if (schoolRestrictions != null && !schoolRestrictions.isEmpty()) {
            SchoolType spellSchool = spell.getSchoolType();
            if (spellSchool != null && schoolRestrictions.contains(spellSchool.getId())) {
                return true;
            }
        }
        
        // If nothing is configured for this subclass, allow by default
        if (subclassRestrictions == null && schoolRestrictions == null) {
            return true;
        }
        
        // Spell didn't match any allowed category or school
        return false;
    }

    public static Component getRestrictionMessage(Player player, AbstractSpell spell) {
        PlayerClassInfo classInfo = getPlayerClassInfo(player);
        if (classInfo == null) {
            return Component.translatable("otherworldorigins.restriction.unknown")
                    .withStyle(ChatFormatting.DARK_GRAY);
        }

        String fullSubclassKey = classInfo.className + "/" + classInfo.subclassName;
        Set<SpellCategory> allowedCategories = SUBCLASS_SPELL_RESTRICTIONS.get(fullSubclassKey);
        Set<ResourceLocation> allowedSchools = SUBCLASS_SCHOOL_RESTRICTIONS.get(fullSubclassKey);

        String formattedSubclass = Arrays.stream(classInfo.subclassName.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));

        String formattedClass = classInfo.className.substring(0, 1).toUpperCase() +
                classInfo.className.substring(1).toLowerCase() + "s";
        
        Component subclassComponent = Component.literal(formattedSubclass + " " + formattedClass);

        boolean hasCategories = allowedCategories != null && !allowedCategories.isEmpty();
        boolean hasSchools = allowedSchools != null && !allowedSchools.isEmpty();
        
        if (!hasCategories && !hasSchools) {
            return Component.translatable("otherworldorigins.restriction.no_spells", subclassComponent)
                    .withStyle(ChatFormatting.DARK_GRAY);
        }

        // Build combined list of components (categories plain, schools colored)
        List<Component> allowedTypes = new ArrayList<>();
        
        if (hasCategories) {
            for (SpellCategory cat : allowedCategories) {
                String catName = cat.toString().substring(0, 1).toUpperCase() + cat.toString().substring(1).toLowerCase();
                allowedTypes.add(Component.literal(catName));
            }
        }
        
        if (hasSchools) {
            for (ResourceLocation schoolLoc : allowedSchools) {
                SchoolType schoolType = getSchoolType(schoolLoc);
                if (schoolType != null) {
                    Style schoolStyle = schoolType.getDisplayName().getStyle();
                    String schoolName = schoolLoc.getPath().substring(0, 1).toUpperCase() + 
                            schoolLoc.getPath().substring(1).toLowerCase();
                    allowedTypes.add(Component.literal(schoolName).withStyle(schoolStyle));
                }
            }
        }

        Component listComponent = formatComponentList(allowedTypes);
        return Component.translatable("otherworldorigins.restriction.can_only_cast", subclassComponent, listComponent)
                .withStyle(ChatFormatting.DARK_GRAY);
    }
    
    private static SchoolType getSchoolType(ResourceLocation id) {
        if (SchoolRegistry.REGISTRY.get() == null) return null;
        return SchoolRegistry.REGISTRY.get().getValue(id);
    }
    
    /**
     * Formats a list of components with proper English grammar using translation keys.
     * 1 item: "X"
     * 2 items: "X or Y"
     * 3+ items: "X, Y, or Z"
     */
    private static Component formatComponentList(List<Component> items) {
        if (items.isEmpty()) {
            return Component.empty();
        }
        
        if (items.size() == 1) {
            return items.get(0);
        }
        
        if (items.size() == 2) {
            return Component.translatable("otherworldorigins.restriction.list.two", items.get(0), items.get(1));
        }
        
        // 3+ items: "X, Y, Z, or W"
        MutableComponent result = Component.empty();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                if (i == items.size() - 1) {
                    result.append(Component.translatable("otherworldorigins.restriction.list.last_separator"));
                } else {
                    result.append(Component.translatable("otherworldorigins.restriction.list.separator"));
                }
            }
            result.append(items.get(i));
        }
        return result;
    }

    public static void clearCache(Player player) {
        playerClassCache.remove(player.getUUID());
    }
}