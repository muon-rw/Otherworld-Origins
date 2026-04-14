package dev.muon.otherworldorigins.util;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;

/**
 * Maps Way of the Four Elements discipline origin paths (registry path under
 * {@code otherworldorigins}) to Irons Spellbooks spell ids for UI: spell guide
 * text and spell icon textures (same pattern as cantrip origins).
 */
public final class ElementalDisciplineSpellDisplay {

    private static final Map<String, String> DISCIPLINE_SLUG_TO_SPELL = Map.ofEntries(
            Map.entry("breath_of_winter", "cone_of_cold"),
            Map.entry("flames_of_the_phoenix", "fireball"),
            Map.entry("river_of_hungry_flame", "wall_of_fire"),
            Map.entry("rush_of_the_gale_spirits", "gust"),
            Map.entry("sweeping_cinder_strike", "blaze_storm"),
            Map.entry("water_whip", "traveloptics:tidal_grasp"),
            Map.entry("eternal_mountain_defense", "oakskin"),
            Map.entry("fist_of_four_thunders", "shockwave"),
            Map.entry("shape_the_flowing_river", "traveloptics:overflow"),
            Map.entry("fangs_of_the_fire_snake", "flaming_strike")
    );

    private ElementalDisciplineSpellDisplay() {
    }

    /**
     * @param originPath origin registry path, e.g. {@code elemental_discipline_one/breath_of_winter}
     */
    public static Optional<ResourceLocation> spellIdForDisciplineOriginPath(String originPath) {
        if (!originPath.startsWith("elemental_discipline_")) {
            return Optional.empty();
        }
        int slash = originPath.indexOf('/');
        if (slash < 0 || slash >= originPath.length() - 1) {
            return Optional.empty();
        }
        String slug = originPath.substring(slash + 1);
        String spell = DISCIPLINE_SLUG_TO_SPELL.get(slug);
        if (spell == null) {
            return Optional.empty();
        }
        if (spell.indexOf(':') >= 0) {
            return Optional.of(ResourceLocation.parse(spell));
        }
        String ns = resolveSpellNamespace(spell);
        return Optional.of(ResourceLocation.fromNamespaceAndPath(ns, spell));
    }

    /** Spell registry path (texture / {@link #resolveSpellNamespace} key), e.g. {@code cone_of_cold}. */
    public static Optional<String> spellPathForDisciplineOrigin(String originPath) {
        return spellIdForDisciplineOriginPath(originPath).map(ResourceLocation::getPath);
    }

    public static MutableComponent appendSpellGuide(MutableComponent desc, ResourceLocation spellId) {
        Component spellDesc = Component.translatable(
                        "spell." + spellId.getNamespace() + "." + spellId.getPath() + ".guide")
                .withStyle(style -> style.withItalic(true));
        desc.append("\n\n").append(spellDesc);
        return desc;
    }

    public static String resolveSpellNamespace(String spellPath) {
        for (AbstractSpell spell : SpellRegistry.REGISTRY.get().getValues()) {
            if (spell.getSpellResource().getPath().equals(spellPath)) {
                return spell.getSpellResource().getNamespace();
            }
        }
        return IronsSpellbooks.MODID;
    }
}
