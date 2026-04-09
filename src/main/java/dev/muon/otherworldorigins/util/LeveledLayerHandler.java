package dev.muon.otherworldorigins.util;

import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.network.CloseCurrentScreenMessage;
import dev.muon.otherworldorigins.network.SendLeveledLayersMessage;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public final class LeveledLayerHandler {

    private LeveledLayerHandler() {}

    public static void checkForEmptyValidLayers(ServerPlayer player) {
        Registry<OriginLayer> layerRegistry = OriginsAPI.getLayersRegistry(player.level().getServer());
        IOriginContainer originContainer = IOriginContainer.get(player).resolve().orElse(null);

        if (originContainer == null) return;

        List<ResourceLocation> availableLayers = new ArrayList<>();

        for (ResourceLocation layerId : LeveledLayers.IDS) {
            ResourceKey<OriginLayer> layerKey = ResourceKey.create(layerRegistry.key(), layerId);
            Holder<OriginLayer> layerHolder = layerRegistry.getHolder(layerKey).orElse(null);

            if (layerHolder != null) {
                OriginLayer layer = layerHolder.value();
                ResourceKey<Origin> currentOrigin = originContainer.getOrigin(layerHolder);

                if (currentOrigin.location().equals(ResourceLocation.fromNamespaceAndPath("origins", "empty")) && !layer.origins(player).isEmpty()) {
                    availableLayers.add(layerId);
                }
            }
        }

        if (!availableLayers.isEmpty()) {
            player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
            PacketDistributor.PacketTarget target = PacketDistributor.PLAYER.with(() -> player);
            OtherworldOrigins.CHANNEL.send(target, new CloseCurrentScreenMessage());
            OtherworldOrigins.CHANNEL.send(target, new SendLeveledLayersMessage(availableLayers));
        }
    }
}
