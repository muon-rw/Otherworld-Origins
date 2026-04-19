package dev.muon.otherworldorigins.util;

import dev.muon.otherworldorigins.OtherworldOrigins;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Origin layers that are level-gated: checked for empty-but-valid on level-up, and reset together on aptitude respec.
 */
public final class LeveledLayers {

    private LeveledLayers() {}

    public static final List<ResourceLocation> IDS = List.of(
            OtherworldOrigins.loc("first_feat"),
            OtherworldOrigins.loc("second_feat"),
            OtherworldOrigins.loc("third_feat"),
            OtherworldOrigins.loc("fourth_feat"),
            OtherworldOrigins.loc("fifth_feat"),
            OtherworldOrigins.loc("plus_one_aptitude_resilient"),
            OtherworldOrigins.loc("chemical_mastery"),
            OtherworldOrigins.loc("magical_secrets"),
            OtherworldOrigins.loc("elemental_discipline_one"),
            OtherworldOrigins.loc("elemental_discipline_two"),
            OtherworldOrigins.loc("elemental_discipline_three"),
            OtherworldOrigins.loc("elemental_discipline_four")
    );
}
