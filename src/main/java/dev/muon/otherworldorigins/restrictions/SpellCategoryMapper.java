package dev.muon.otherworldorigins.restrictions;

import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.config.OtherworldOriginsConfig;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * Maps spells to their categories. Handles loading from config and provides category lookups.
 */
public class SpellCategoryMapper {
    private static final Map<ResourceLocation, Set<SpellCategory>> spellCategoryMap = new HashMap<>();
    
    /**
     * Initializes the mapper from config. Can be called multiple times to reload.
     */
    public static void initialize() {
        spellCategoryMap.clear();
        
        processSpellList(OtherworldOriginsConfig.OFFENSIVE_SPELLS.get(), SpellCategory.OFFENSIVE);
        processSpellList(OtherworldOriginsConfig.SUPPORT_SPELLS.get(), SpellCategory.SUPPORT);
        processSpellList(OtherworldOriginsConfig.DEFENSIVE_SPELLS.get(), SpellCategory.DEFENSIVE);
    }
    
    private static void processSpellList(List<? extends String> spellList, SpellCategory category) {
        for (String spellId : spellList) {
            try {
                ResourceLocation resourceLocation = parseResourceLocation(spellId);
                
                if (SpellRegistry.getSpell(resourceLocation) == SpellRegistry.none()) {
                    OtherworldOrigins.LOGGER.warn("Spell {} not found in SpellRegistry", spellId);
                    continue;
                }
                
                spellCategoryMap.computeIfAbsent(resourceLocation, k -> new HashSet<>()).add(category);
            } catch (Exception e) {
                OtherworldOrigins.LOGGER.error("Invalid spell ID in config: {}", spellId, e);
            }
        }
    }
    
    /**
     * Gets all categories for a spell. Returns a set containing OFFENSIVE if no mapping exists.
     */
    public static Set<SpellCategory> getCategories(AbstractSpell spell) {
        ResourceLocation spellId = spell.getSpellResource();
        Set<SpellCategory> categories = spellCategoryMap.get(spellId);
        
        if (categories == null || categories.isEmpty()) {
            OtherworldOrigins.LOGGER.warn("No category mapping found for spell: {}, assuming it's a Damaging spell. Update the otherworldorigins-common config!", spellId);
            return Set.of(SpellCategory.OFFENSIVE);
        }
        
        return Collections.unmodifiableSet(categories);
    }
    
    /**
     * Parses a resource location string, defaulting to "irons_spellbooks" namespace if not specified.
     */
    private static ResourceLocation parseResourceLocation(String id) {
        if (!id.contains(":")) {
            return ResourceLocation.fromNamespaceAndPath("irons_spellbooks", id);
        }
        return ResourceLocation.parse(id);
    }
}
