package dev.muon.raven_dnd_origins.network;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.builtin.ResourcePower;
import dev.overgrown.origins.component.PlayerOriginsAttachment;
import dev.overgrown.origins.component.PlayerOriginsImpl;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record WildshapeCantripHeldMessage(boolean held) implements CustomPacketPayload {

    private static final ResourceLocation WILDSHAPE_LAYER_ID = RavenDndOrigins.loc("wildshape");
    private static final ResourceLocation CANTRIP_HELD_ID = RavenDndOrigins.loc("class/druid/wildshape_cantrip_held");

    public static final Type<WildshapeCantripHeldMessage> TYPE =
            new Type<>(RavenDndOrigins.loc("wildshape_cantrip_held"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WildshapeCantripHeldMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, WildshapeCantripHeldMessage::held,
                    WildshapeCantripHeldMessage::new);

    public static void handle(WildshapeCantripHeldMessage message, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) {
            return;
        }
        ctx.enqueueWork(() -> {
            if (!hasActiveWildshape(player)) {
                setCantripHeld(player, false);
                return;
            }
            setCantripHeld(player, message.held());
        });
    }

    private static boolean hasActiveWildshape(ServerPlayer player) {
        PlayerOriginsImpl state = PlayerOriginsAttachment.get(player);
        return state != null && state.hasOrigin(WILDSHAPE_LAYER_ID);
    }

    private static void setCantripHeld(ServerPlayer player, boolean held) {
        PowerContainer container = PowerContainer.of(player);
        if (container == null) {
            return;
        }
        ResourcePower.writeValue(container, CANTRIP_HELD_ID, held ? 1 : 0);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
