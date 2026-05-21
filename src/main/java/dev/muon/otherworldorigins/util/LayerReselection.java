package dev.muon.otherworldorigins.util;

import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.network.CloseCurrentScreenMessage;
import dev.muon.otherworldorigins.network.SendLeveledLayersMessage;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import io.github.edwinmindcraft.origins.common.OriginsCommon;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Clears a fixed set of origin layers and reopens the selection screen for only those layers.
 * The screen runs in dynamic-prompt mode, so it skips the final confirmation step that the full
 * character creation uses. Backs the Orb of Ancestry and Orb of Vocation items.
 */
public final class LayerReselection {

    private LayerReselection() {}

    /**
     * Race, subrace, and every layer whose available options are gated on the race or subrace
     * choice. {@code plus_one_aptitude_resilient} is included because {@code free_feat} can grant
     * the Resilient feat that gates it; clearing it here avoids an orphaned ability bonus when the
     * free feat changes. Ordered by layer order so the screen evaluates parents before dependents.
     */
    public static final List<ResourceLocation> ANCESTRY_LAYERS = List.of(
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
     * choice. Ordered by layer order so the screen evaluates parents before their dependents.
     */
    public static final List<ResourceLocation> VOCATION_LAYERS = List.of(
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

    /**
     * Clears the given layers for the player and opens the selection screen for them.
     * Dependent layers fall to empty automatically once their parent layer is empty, so the
     * screen simply skips any layer that has no valid options for the current selections.
     *
     * @return true if reselection started; false if the player has no origin container or no
     *         given layer is registered (in which case the caller should not consume the orb).
     */
    public static boolean begin(ServerPlayer player, List<ResourceLocation> layers) {
        IOriginContainer container = IOriginContainer.get(player).resolve().orElse(null);
        if (container == null) return false;

        Registry<OriginLayer> layerRegistry = OriginsAPI.getLayersRegistry(player.server);
        List<ResourceLocation> clearedLayers = new ArrayList<>();
        for (ResourceLocation layerId : layers) {
            ResourceKey<OriginLayer> key = ResourceKey.create(layerRegistry.key(), layerId);
            Holder<OriginLayer> layerHolder = layerRegistry.getHolder(key).orElse(null);
            if (layerHolder == null) {
                OtherworldOrigins.LOGGER.error("layer_reselection: layer '{}' does not exist", layerId);
                continue;
            }
            container.setOrigin(layerHolder.value(), Origin.EMPTY);
            clearedLayers.add(layerId);
        }

        if (clearedLayers.isEmpty()) return false;

        container.checkAutoChoosingLayers(false);

        PacketDistributor.PacketTarget target = PacketDistributor.PLAYER.with(() -> player);
        OriginsCommon.CHANNEL.send(target, container.getSynchronizationPacket());
        container.synchronize();

        OtherworldOrigins.LOGGER.debug("layer_reselection: cleared {} layer(s) for {}: {}",
                clearedLayers.size(), player.getName().getString(), clearedLayers);

        OtherworldOrigins.CHANNEL.send(target, new CloseCurrentScreenMessage());
        OtherworldOrigins.CHANNEL.send(target, new SendLeveledLayersMessage(clearedLayers));
        return true;
    }
}
