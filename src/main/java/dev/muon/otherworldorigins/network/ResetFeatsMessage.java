package dev.muon.otherworldorigins.network;

import dev.muon.otherworldorigins.OtherworldOrigins;
import io.github.apace100.origins.registry.ModComponents;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.origin.OriginLayers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class ResetFeatsMessage {
    private static final List<ResourceLocation> FEAT_LAYERS = Arrays.asList(
            OtherworldOrigins.loc("first_feat"),
            OtherworldOrigins.loc("second_feat"),
            OtherworldOrigins.loc("third_feat"),
            OtherworldOrigins.loc("fourth_feat"),
            OtherworldOrigins.loc("fifth_feat")
    );

    public ResetFeatsMessage() {}

    public static ResetFeatsMessage decode(FriendlyByteBuf buf) {
        return new ResetFeatsMessage();
    }

    public void encode(FriendlyByteBuf buf) {}

    public static void handle(ResetFeatsMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                OriginComponent originComponent = ModComponents.ORIGIN.maybeGet(player).orElse(null);
                if (originComponent != null) {
                    for (OriginLayer layer : OriginLayers.getLayers()) {
                        if (!layer.isEnabled()) continue;
                        ResourceLocation layerId = layer.getIdentifier();
                        if (FEAT_LAYERS.contains(layerId)) {
                            originComponent.setOrigin(layer, Origin.EMPTY);
                        }
                    }
                    // Note: sync() handles synchronization in native Origins
                    originComponent.sync();
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public static void send() {
        OtherworldOrigins.CHANNEL.sendToServer(new ResetFeatsMessage());
    }
}
