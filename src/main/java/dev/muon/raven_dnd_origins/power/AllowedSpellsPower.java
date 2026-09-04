package dev.muon.raven_dnd_origins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.overgrown.apoli.power.PowerType;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class AllowedSpellsPower extends PowerType<AllowedSpellsPower.Configuration> {

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf("entries", List.of()).forGetter(Configuration::entries)
    ).apply(instance, Configuration::new));

    @Override
    public MapCodec<Configuration> configCodec() {
        return CODEC;
    }

    public record Configuration(List<String> entries) {

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
            TagKey<AbstractSpell> tagKey = TagKey.create(SpellRegistry.SPELL_REGISTRY_KEY, loc);
            Optional<HolderSet.Named<AbstractSpell>> tag = SpellRegistry.REGISTRY.getTag(tagKey);
            if (tag.isEmpty()) {
                warnOnce("tag:" + tagId, "Spell tag not bound, cannot resolve tag " + tagId);
                return false;
            }
            for (Holder<AbstractSpell> holder : tag.get()) {
                if (holder.value() == spell) return true;
            }
            return false;
        }

        private boolean matchesSchool(AbstractSpell spell, String schoolId) {
            ResourceLocation loc = parseLoc(schoolId);
            if (loc == null) {
                warnOnce("school:" + schoolId, "Invalid school ID: " + schoolId);
                return false;
            }
            SchoolType school = SchoolRegistry.getSchool(loc);
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
                RavenDndOrigins.LOGGER.warn("[AllowedSpellsPower] {}", message);
            }
        }
    }
}
