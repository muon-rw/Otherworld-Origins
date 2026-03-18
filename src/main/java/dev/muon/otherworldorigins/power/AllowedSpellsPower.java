package dev.muon.otherworldorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.otherworldorigins.OtherworldOrigins;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraftforge.registries.tags.ITagManager;

import java.util.*;

public class AllowedSpellsPower extends PowerFactory<AllowedSpellsPower.Configuration> {

    public AllowedSpellsPower() {
        super(Configuration.CODEC);
    }

    public record Configuration(List<String> entries) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.listOf().optionalFieldOf("entries", List.of()).forGetter(Configuration::entries)
        ).apply(instance, Configuration::new));

        @Override
        public boolean isConfigurationValid() {
            return true;
        }

        private static final Set<String> warnedEntries = Collections.synchronizedSet(new HashSet<>());

        public boolean isSpellAllowed(AbstractSpell spell) {
            for (String entry : entries) {
                if (entry.startsWith("#")) {
                    if (matchesTag(spell, entry.substring(1))) return true;
                } else if (entry.startsWith("@")) {
                    if (matchesSchool(spell, entry.substring(1))) return true;
                } else {
                    if (matchesSpell(spell, entry)) return true;
                }
            }
            return false;
        }

        private boolean matchesTag(AbstractSpell spell, String tagId) {
            ResourceLocation loc = parseLoc(tagId);
            if (loc == null) {
                warnOnce("tag:" + tagId, "Invalid tag ID: " + tagId);
                return false;
            }
            ITagManager<AbstractSpell> tagManager = SpellRegistry.REGISTRY.get().tags();
            if (tagManager == null) {
                warnOnce("tag:" + tagId, "Spell tag manager unavailable, cannot resolve tag " + tagId);
                return false;
            }
            TagKey<AbstractSpell> tagKey = TagKey.create(SpellRegistry.SPELL_REGISTRY_KEY, loc);
            return tagManager.getTag(tagKey).contains(spell);
        }

        private boolean matchesSchool(AbstractSpell spell, String schoolId) {
            ResourceLocation loc = parseLoc(schoolId);
            if (loc == null) {
                warnOnce("school:" + schoolId, "Invalid school ID: " + schoolId);
                return false;
            }
            if (SchoolRegistry.REGISTRY.get() == null) return false;
            SchoolType school = SchoolRegistry.REGISTRY.get().getValue(loc);
            if (school == null) {
                warnOnce("school:" + schoolId, "School not found in registry: " + schoolId);
                return false;
            }
            SchoolType spellSchool = spell.getSchoolType();
            return spellSchool != null && spellSchool.getId().equals(loc);
        }

        private boolean matchesSpell(AbstractSpell spell, String spellId) {
            ResourceLocation loc = parseLoc(spellId);
            if (loc == null) {
                warnOnce("spell:" + spellId, "Invalid spell ID: " + spellId);
                return false;
            }
            AbstractSpell target = SpellRegistry.getSpell(loc);
            if (target == SpellRegistry.none()) {
                warnOnce("spell:" + spellId, "Spell not found in registry: " + spellId);
                return false;
            }
            return spell == target;
        }

        private static ResourceLocation parseLoc(String id) {
            try {
                return id.contains(":") ? ResourceLocation.tryParse(id) : ResourceLocation.fromNamespaceAndPath("irons_spellbooks", id);
            } catch (Exception e) {
                return null;
            }
        }

        private static void warnOnce(String key, String message) {
            if (warnedEntries.add(key)) {
                OtherworldOrigins.LOGGER.warn("[AllowedSpellsPower] {}", message);
            }
        }
    }
}
