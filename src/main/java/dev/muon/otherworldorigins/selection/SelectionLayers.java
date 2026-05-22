package dev.muon.otherworldorigins.selection;

import dev.muon.otherworldorigins.OtherworldOrigins;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * The canonical origin-layer groupings used by the selection-session system. Single source of
 * truth — replaces the per-class copies that previously drifted apart.
 */
public final class SelectionLayers {

    private SelectionLayers() {}

    /**
     * Level-gated layers: scanned for empty-but-valid on level-up, and reset together on aptitude
     * respec.
     */
    public static final List<ResourceLocation> LEVEL_GATED = List.of(
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

    /**
     * Race, subrace, and every layer whose available options are gated on the race or subrace
     * choice — the Orb of Ancestry layer set. {@code plus_one_aptitude_resilient} is included
     * because {@code free_feat} can grant the Resilient feat that gates it; clearing it here
     * avoids an orphaned ability bonus when the free feat changes. Ordered by layer order so the
     * screen evaluates parents before dependents.
     */
    public static final List<ResourceLocation> ANCESTRY = List.of(
            OtherworldOrigins.loc("race"),
            OtherworldOrigins.loc("plus_one_aptitude_one"),
            OtherworldOrigins.loc("plus_one_aptitude_two"),
            OtherworldOrigins.loc("subrace"),
            OtherworldOrigins.loc("plus_two_aptitude_one"),
            OtherworldOrigins.loc("plus_two_aptitude_two"),
            OtherworldOrigins.loc("free_feat"),
            OtherworldOrigins.loc("cantrip_one"),
            OtherworldOrigins.loc("plus_one_aptitude_resilient")
    );

    /**
     * Class, subclass, and every layer whose available options are gated on the class or subclass
     * choice — the Orb of Vocation layer set. Ordered by layer order so the screen evaluates
     * parents before their dependents.
     */
    public static final List<ResourceLocation> VOCATION = List.of(
            OtherworldOrigins.loc("class"),
            OtherworldOrigins.loc("subclass"),
            OtherworldOrigins.loc("draconic_ancestry"),
            OtherworldOrigins.loc("cantrip_two"),
            OtherworldOrigins.loc("elemental_discipline_one"),
            OtherworldOrigins.loc("elemental_discipline_two"),
            OtherworldOrigins.loc("elemental_discipline_three"),
            OtherworldOrigins.loc("elemental_discipline_four"),
            OtherworldOrigins.loc("magical_secrets"),
            OtherworldOrigins.loc("chemical_mastery"),
            OtherworldOrigins.loc("wildshape")
    );
}
