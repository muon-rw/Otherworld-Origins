package dev.muon.otherworldorigins.network;

import dev.muon.otherworldorigins.OtherworldOrigins;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
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
import net.minecraftforge.network.PacketDistributor;

import java.util.Collections;
import java.util.function.Supplier;

public class WildshapeKeyMessage {

    private static final ResourceLocation WILDSHAPE_LAYER_ID = OtherworldOrigins.loc("wildshape");
    private static final ResourceLocation WILDSHAPE_CHARGES_ID =
            ResourceLocation.fromNamespaceAndPath("otherworldorigins", "class/druid/wildshape_wildshape_charges");
    private static final ResourceLocation WILDSHAPE_GATE_ID =
            ResourceLocation.fromNamespaceAndPath("otherworldorigins", "class/druid/wildshape_wildshape_gate");

    public WildshapeKeyMessage() {}

    public static void encode(WildshapeKeyMessage message, FriendlyByteBuf buffer) {}

    public static WildshapeKeyMessage decode(FriendlyByteBuf buffer) {
        return new WildshapeKeyMessage();
    }

    public static void handle(WildshapeKeyMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            IOriginContainer originContainer = IOriginContainer.get(player).resolve().orElse(null);
            if (originContainer == null) return;

            Registry<OriginLayer> layerRegistry = OriginsAPI.getLayersRegistry(player.level().getServer());
            ResourceKey<OriginLayer> layerKey = ResourceKey.create(
                    OriginsDynamicRegistries.LAYERS_REGISTRY, WILDSHAPE_LAYER_ID);
            Holder<OriginLayer> layerHolder = layerRegistry.getHolder(layerKey).orElse(null);
            if (layerHolder == null) return;

            ResourceKey<Origin> currentOrigin = originContainer.getOrigin(layerHolder);
            boolean hasActiveShape = currentOrigin != null
                    && !currentOrigin.location().equals(ResourceLocation.fromNamespaceAndPath("origins", "empty"));

            if (hasActiveShape) {
                ResourceKey<Origin> emptyKey = ResourceKey.create(
                        OriginsDynamicRegistries.ORIGINS_REGISTRY,
                        ResourceLocation.fromNamespaceAndPath("origins", "empty"));
                setResourceValue(player, WILDSHAPE_GATE_ID, 0);
                originContainer.setOrigin(layerKey, emptyKey);
                originContainer.checkAutoChoosingLayers(false);
                ApoliAPI.synchronizePowerContainer(player);
                originContainer.synchronize();
            } else {
                int charges = getResourceValue(player, WILDSHAPE_CHARGES_ID);
                if (charges <= 0) return;

                setResourceValue(player, WILDSHAPE_GATE_ID, 1);
                ApoliAPI.synchronizePowerContainer(player);
                originContainer.synchronize();

                PacketDistributor.PacketTarget target = PacketDistributor.PLAYER.with(() -> player);
                OtherworldOrigins.CHANNEL.send(target, new CloseCurrentScreenMessage());
                OtherworldOrigins.CHANNEL.send(target, new SendFeatLayersMessage(
                        Collections.singletonList(WILDSHAPE_LAYER_ID)));
            }
        });
        context.setPacketHandled(true);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int getResourceValue(ServerPlayer player, ResourceLocation resourceId) {
        IPowerContainer container = IPowerContainer.get(player).resolve().orElse(null);
        if (container == null) return 0;

        Holder power = container.getPower(resourceId);
        if (power != null && ((ConfiguredPower) power.value()).getFactory() instanceof VariableIntPowerFactory factory) {
            return factory.getValue((ConfiguredPower) power.value(), player);
        }
        return 0;
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
