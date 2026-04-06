package dev.muon.otherworldorigins.network;

import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.apoli.api.power.factory.power.VariableIntPowerFactory;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import io.github.edwinmindcraft.origins.api.registry.OriginsDynamicRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class WildshapeCantripHeldMessage {

    private static final ResourceLocation WILDSHAPE_LAYER_ID =
            ResourceLocation.fromNamespaceAndPath("otherworldorigins", "wildshape");
    private static final ResourceLocation CANTRIP_HELD_ID =
            ResourceLocation.fromNamespaceAndPath("otherworldorigins", "class/druid/wildshape_cantrip_held");

    private final boolean held;

    public WildshapeCantripHeldMessage(boolean held) {
        this.held = held;
    }

    public static void encode(WildshapeCantripHeldMessage message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.held);
    }

    public static WildshapeCantripHeldMessage decode(FriendlyByteBuf buffer) {
        return new WildshapeCantripHeldMessage(buffer.readBoolean());
    }

    public static void handle(WildshapeCantripHeldMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            if (!hasActiveWildshape(player)) {
                setResourceValue(player, CANTRIP_HELD_ID, 0);
                return;
            }

            setResourceValue(player, CANTRIP_HELD_ID, message.held ? 1 : 0);
        });
        context.setPacketHandled(true);
    }

    private static boolean hasActiveWildshape(ServerPlayer player) {
        IOriginContainer originContainer = IOriginContainer.get(player).resolve().orElse(null);
        if (originContainer == null) return false;

        Registry<OriginLayer> layerRegistry = OriginsAPI.getLayersRegistry(player.level().getServer());
        ResourceKey<OriginLayer> layerKey = ResourceKey.create(
                OriginsDynamicRegistries.LAYERS_REGISTRY, WILDSHAPE_LAYER_ID);
        Holder<OriginLayer> layerHolder = layerRegistry.getHolder(layerKey).orElse(null);
        if (layerHolder == null) return false;

        ResourceKey<Origin> currentOrigin = originContainer.getOrigin(layerHolder);
        return currentOrigin != null
                && !currentOrigin.location().equals(ResourceLocation.fromNamespaceAndPath("origins", "empty"));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setResourceValue(ServerPlayer player, ResourceLocation resourceId, int value) {
        IPowerContainer container = IPowerContainer.get(player).resolve().orElse(null);
        if (container == null) return;

        Holder power = container.getPower(resourceId);
        if (power != null && ((ConfiguredPower) power.value()).getFactory() instanceof VariableIntPowerFactory factory) {
            factory.assign((ConfiguredPower) power.value(), player, value);
        }
    }
}
