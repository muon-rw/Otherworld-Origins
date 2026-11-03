package dev.muon.raven_dnd_origins.util;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Maps Way of the Four Elements discipline origin paths (registry path under
 * {@code raven_dnd_origins}) to Irons Spellbooks spell ids for UI: spell guide
 * text and spell icon textures (same pattern as cantrip origins).
 */
public final class ElementalDisciplineSpellDisplay {

    private static final Map<String, String> DISCIPLINE_SLUG_TO_SPELL = Map.ofEntries(
            Map.entry("breath_of_winter", "cone_of_cold"),
            Map.entry("flames_of_the_phoenix", "fireball"),
            Map.entry("river_of_hungry_flame", "wall_of_fire"),
            Map.entry("rush_of_the_gale_spirits", "gust"),
            Map.entry("sweeping_cinder_strike", "fire_breath"),
            Map.entry("water_whip", "cataclysm_spellbooks:tidal_grab"),
            Map.entry("eternal_mountain_defense", "oakskin"),
            Map.entry("fist_of_four_thunders", "shockwave"),
            Map.entry("shape_the_flowing_river", "aero_additions:flush"),
            Map.entry("fangs_of_the_fire_snake", "flaming_strike"),
            Map.entry("clench_of_the_north_wind", "discerning_the_eldritch:glacial_edge"),
            Map.entry("fist_of_unbroken_air", "aero_additions:wind_blade"),
            Map.entry("ride_the_wind", "aero_additions:feather_fall")
    );

    // Cantrip origin files must be named after the spell path they cast, since the icon and guide text are looked up by it.
    private static final List<String> CANTRIP_PREFIXES = List.of("cantrips/two/", "cantrips/magical_secrets/", "cantrips/");

    private ElementalDisciplineSpellDisplay() {
    }

    public static Optional<ResourceLocation> spellIdForCantripOriginPath(String originPath) {
        for (String prefix : CANTRIP_PREFIXES) {
            if (originPath.startsWith(prefix)) {
                String slug = originPath.substring(prefix.length());
                return Optional.of(ResourceLocation.fromNamespaceAndPath(resolveSpellNamespace(slug), slug));
            }
        }
        return Optional.empty();
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

    /** Spell icons and guide text are keyed by path only, and our screens look them up every frame, so we memoize the registry scan. */
    private static final java.util.Map<String, String> SPELL_NAMESPACES = new java.util.concurrent.ConcurrentHashMap<>();

    public static String resolveSpellNamespace(String spellPath) {
        return SPELL_NAMESPACES.computeIfAbsent(spellPath, path -> {
            for (AbstractSpell spell : SpellRegistry.REGISTRY) {
                if (spell.getSpellResource().getPath().equals(path)) {
                    return spell.getSpellResource().getNamespace();
                }
            }
            return IronsSpellbooks.MODID;
        });
    }
}
