package dev.muon.otherworldorigins.util;

import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.network.CloseCurrentScreenMessage;
import dev.muon.otherworldorigins.network.SendFeatLayersMessage;
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
import java.util.Arrays;
import java.util.List;

public class FeatHandler {

    private static final List<ResourceLocation> FEAT_LAYERS = Arrays.asList(
            OtherworldOrigins.loc("first_feat"),
            OtherworldOrigins.loc("second_feat"),
            OtherworldOrigins.loc("third_feat"),
            OtherworldOrigins.loc("fourth_feat"),
            OtherworldOrigins.loc("fifth_feat")
    );

    public static void checkForFeats(ServerPlayer player) {
        Registry<OriginLayer> layerRegistry = OriginsAPI.getLayersRegistry(player.level().getServer());
        IOriginContainer originContainer = IOriginContainer.get(player).resolve().orElse(null);

        if (originContainer == null) return;

        List<ResourceLocation> availableFeatLayers = new ArrayList<>();

        for (ResourceLocation layerId : FEAT_LAYERS) {
            ResourceKey<OriginLayer> layerKey = ResourceKey.create(layerRegistry.key(), layerId);
            Holder<OriginLayer> layerHolder = layerRegistry.getHolder(layerKey).orElse(null);

            if (layerHolder != null) {
                OriginLayer layer = layerHolder.value();
                ResourceKey<Origin> currentOrigin = originContainer.getOrigin(layerHolder);

                if (currentOrigin.location().equals(new ResourceLocation("origins", "empty")) && !layer.origins(player).isEmpty()) {
                    availableFeatLayers.add(layerId);
                }
            }
        }

        if (!availableFeatLayers.isEmpty()) {
            player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
            PacketDistributor.PacketTarget target = PacketDistributor.PLAYER.with(() -> player);
            OtherworldOrigins.CHANNEL.send(target, new CloseCurrentScreenMessage());
            OtherworldOrigins.CHANNEL.send(target, new SendFeatLayersMessage(availableFeatLayers));
        }
    }
}
