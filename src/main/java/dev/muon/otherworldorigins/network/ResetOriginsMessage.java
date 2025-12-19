package dev.muon.otherworldorigins.network;

import dev.muon.otherworldorigins.OtherworldOrigins;
import io.github.apace100.origins.registry.ModComponents;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.origin.OriginLayers;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class ResetOriginsMessage {

    public ResetOriginsMessage() {}

    public static ResetOriginsMessage decode(FriendlyByteBuf buf) {
        return new ResetOriginsMessage();
    }

    public void encode(FriendlyByteBuf buf) {}

    public static void handle(ResetOriginsMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                OriginComponent originComponent = ModComponents.ORIGIN.maybeGet(player).orElse(null);
                if (originComponent != null) {
                    for (OriginLayer layer : OriginLayers.getLayers()) {
                        if (layer.isEnabled()) {
                            originComponent.setOrigin(layer, Origin.EMPTY);
                        }
                    }
                    originComponent.selectingOrigin(true);
                    originComponent.checkAutoChoosingLayers(player, false);
                    originComponent.sync();
                    
                    // Open origin screen
                    PacketDistributor.PacketTarget target = PacketDistributor.PLAYER.with(() -> player);
                    OtherworldOrigins.CHANNEL.send(target, new OpenOriginScreenMessage(false));
                }
            }
        });
    }
}
