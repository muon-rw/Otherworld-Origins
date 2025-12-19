package dev.muon.otherworldorigins.util;

import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.network.CloseCurrentScreenMessage;
import dev.muon.otherworldorigins.network.SendFeatLayersMessage;
import io.github.apace100.origins.registry.ModComponents;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.origin.OriginLayers;
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
        OriginComponent originComponent = ModComponents.ORIGIN.maybeGet(player).orElse(null);
        if (originComponent == null) return;

        List<ResourceLocation> availableFeatLayers = new ArrayList<>();

        for (ResourceLocation layerId : FEAT_LAYERS) {
            OriginLayer layer = OriginLayers.getLayer(layerId);
            if (layer == null) continue;

            Origin currentOrigin = originComponent.getOrigin(layer);
            if ((currentOrigin == null || currentOrigin == Origin.EMPTY) && !layer.getOrigins(player).isEmpty()) {
                availableFeatLayers.add(layerId);
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
