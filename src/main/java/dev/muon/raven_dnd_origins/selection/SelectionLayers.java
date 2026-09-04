package dev.muon.raven_dnd_origins.selection;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * The canonical origin-layer groupings used by the selection-session system. Single source of
 * truth; replaces the per-class copies that previously drifted apart.
 */
public final class SelectionLayers {

    private SelectionLayers() {}

    /**
     * Level-gated layers: scanned for empty-but-valid on level-up, and reset together on aptitude
     * respec.
     */
    public static final List<ResourceLocation> LEVEL_GATED = List.of(
            RavenDndOrigins.loc("first_feat"),
            RavenDndOrigins.loc("second_feat"),
            RavenDndOrigins.loc("third_feat"),
            RavenDndOrigins.loc("fourth_feat"),
            RavenDndOrigins.loc("fifth_feat"),
            RavenDndOrigins.loc("plus_one_aptitude_resilient"),
            RavenDndOrigins.loc("chemical_mastery"),
            RavenDndOrigins.loc("magical_secrets"),
            RavenDndOrigins.loc("elemental_discipline_one"),
            RavenDndOrigins.loc("elemental_discipline_two"),
            RavenDndOrigins.loc("elemental_discipline_three"),
            RavenDndOrigins.loc("elemental_discipline_four")
    );

    /**
     * Race, subrace, and every layer whose available options are gated on the race or subrace
     * choice: the Orb of Ancestry layer set. {@code plus_one_aptitude_resilient} is included
     * because {@code free_feat} can grant the Resilient feat that gates it; clearing it here
     * avoids an orphaned ability bonus when the free feat changes. Ordered by layer order so the
     * screen evaluates parents before dependents.
     */
    public static final List<ResourceLocation> ANCESTRY = List.of(
            RavenDndOrigins.loc("race"),
            RavenDndOrigins.loc("plus_one_aptitude_one"),
            RavenDndOrigins.loc("plus_one_aptitude_two"),
            RavenDndOrigins.loc("subrace"),
            RavenDndOrigins.loc("plus_two_aptitude_one"),
            RavenDndOrigins.loc("plus_two_aptitude_two"),
            RavenDndOrigins.loc("free_feat"),
            RavenDndOrigins.loc("cantrip_one"),
            RavenDndOrigins.loc("plus_one_aptitude_resilient")
    );

    /**
     * Class, subclass, and every layer whose available options are gated on the class or subclass
     * choice: the Orb of Vocation layer set. Ordered by layer order so the screen evaluates
     * parents before their dependents.
     */
    public static final List<ResourceLocation> VOCATION = List.of(
            RavenDndOrigins.loc("class"),
            RavenDndOrigins.loc("subclass"),
            RavenDndOrigins.loc("draconic_ancestry"),
            RavenDndOrigins.loc("cantrip_two"),
            RavenDndOrigins.loc("elemental_discipline_one"),
            RavenDndOrigins.loc("elemental_discipline_two"),
            RavenDndOrigins.loc("elemental_discipline_three"),
            RavenDndOrigins.loc("elemental_discipline_four"),
            RavenDndOrigins.loc("magical_secrets"),
            RavenDndOrigins.loc("chemical_mastery"),
            RavenDndOrigins.loc("wildshape")
    );
}
