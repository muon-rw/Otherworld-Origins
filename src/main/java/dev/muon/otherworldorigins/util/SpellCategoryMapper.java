package dev.muon.otherworldorigins.util;

import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.config.SpellCategoryConfig;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SpellCategoryMapper {
    private static final Map<ResourceLocation, SpellCategory> spellCategoryMap = new HashMap<>();
    private static boolean initialized = false;

    public static void initialize() {
        if (initialized) return;
        initialized = true;

        Map<ResourceLocation, SpellCategory> tempMap = new HashMap<>();
        Set<ResourceLocation> duplicates = new java.util.HashSet<>();

        processSpellList(SpellCategoryConfig.OFFENSIVE_SPELLS.get(), SpellCategory.OFFENSIVE, tempMap, duplicates);
        processSpellList(SpellCategoryConfig.SUPPORT_SPELLS.get(), SpellCategory.SUPPORT, tempMap, duplicates);
        processSpellList(SpellCategoryConfig.DEFENSIVE_SPELLS.get(), SpellCategory.DEFENSIVE, tempMap, duplicates);

        if (!duplicates.isEmpty()) {
            OtherworldOrigins.LOGGER.warn("The following spells were assigned to multiple categories:");
            for (ResourceLocation spell : duplicates) {
                OtherworldOrigins.LOGGER.warn("  - {} was assigned to multiple categories", spell);
            }
        }

        spellCategoryMap.putAll(tempMap);
    }

    private static void processSpellList(List<? extends String> spellList, SpellCategory category,
                                         Map<ResourceLocation, SpellCategory> tempMap,
                                         Set<ResourceLocation> duplicates) {
        for (String spellId : spellList) {
            try {
                ResourceLocation resourceLocation;
                if (!spellId.contains(":")) {
                    resourceLocation = new ResourceLocation("irons_spellbooks", spellId);
                } else {
                    resourceLocation = new ResourceLocation(spellId);
                }

                if (SpellRegistry.getSpell(resourceLocation) == SpellRegistry.none()) {
                    OtherworldOrigins.LOGGER.warn("Spell {} not found in SpellRegistry", spellId);
                    continue;
                }

                if (tempMap.containsKey(resourceLocation)) {
                    duplicates.add(resourceLocation);
                    continue;
                }

                tempMap.put(resourceLocation, category);
            } catch (Exception e) {
                OtherworldOrigins.LOGGER.error("Invalid spell ID in config: {}", spellId, e);
            }
        }
    }

    public static SpellCategory getCategory(AbstractSpell spell) {
        if (!initialized) {
            initialize();
        }

        ResourceLocation spellId = spell.getSpellResource();
        SpellCategory category = spellCategoryMap.get(spellId);

        if (category == null) {
            OtherworldOrigins.LOGGER.warn("No category mapping found for spell: {}, assuming it's a Damaging spell. Update the otherworldorigins-common config!", spellId);
            return SpellCategory.OFFENSIVE; // Default category
        }

        return category;
    }
}