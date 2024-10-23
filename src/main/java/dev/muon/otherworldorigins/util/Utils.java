package dev.muon.otherworldorigins.util;

import dev.muon.medieval.leveling.LevelingUtils;
import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.network.CloseCurrentScreenMessage;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import io.github.edwinmindcraft.origins.common.OriginsCommon;
import io.github.edwinmindcraft.origins.common.network.S2COpenOriginScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.PacketDistributor;

public class Utils {

    public static void checkAndOpenFeatScreen(ServerPlayer player) {
        ResourceLocation layerId = null;
        int playerLevel = LevelingUtils.getPlayerLevel(player);
        if (playerLevel == 4) layerId = OtherworldOrigins.loc("first_feat");
        else if (playerLevel == 8) layerId = OtherworldOrigins.loc("second_feat");
        else if (playerLevel == 12) layerId = OtherworldOrigins.loc("third_feat");
        else if (playerLevel == 16) layerId = OtherworldOrigins.loc("fourth_feat");
        else if (playerLevel == 20) layerId = OtherworldOrigins.loc("fifth_feat");

        if (layerId != null) {
            Registry<OriginLayer> layerRegistry = OriginsAPI.getLayersRegistry(player.level().getServer());
            ResourceKey<OriginLayer> layerKey = ResourceKey.create(layerRegistry.key(), layerId);
            Holder<OriginLayer> layer = layerRegistry.getHolder(layerKey).orElse(null);

            if (layer != null) {
                IOriginContainer originContainer = IOriginContainer.get(player).resolve().orElse(null);
                if (originContainer != null) {
                    ResourceKey<Origin> currentOrigin = originContainer.getOrigin(layer);
                    if (currentOrigin.location().equals(new ResourceLocation("origins", "empty"))) {
                        player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);

                        PacketDistributor.PacketTarget target = PacketDistributor.PLAYER.with(() -> player);

                        OtherworldOrigins.CHANNEL.send(target, new CloseCurrentScreenMessage());

                        if (player.getServer() == null) return;

                        player.getServer().tell(new net.minecraft.server.TickTask(player.getServer().getTickCount() + 1, () -> {
                            OriginsCommon.CHANNEL.send(target, originContainer.getSynchronizationPacket());
                            OriginsCommon.CHANNEL.send(target, new S2COpenOriginScreen(false));
                            originContainer.synchronize();
                        }));
                    }
                }
            }
        }
    }
}
