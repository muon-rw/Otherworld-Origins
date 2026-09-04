package dev.muon.raven_dnd_origins.network;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.config.RavenDndOriginsConfig;
import dev.muon.raven_dnd_origins.util.StarterKitUtil;
import dev.overgrown.origins.component.PlayerOriginsAttachment;
import dev.overgrown.origins.component.PlayerOriginsImpl;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public record GiveStarterKitMessage() implements CustomPacketPayload {
    private static final Logger LOGGER = LoggerFactory.getLogger(GiveStarterKitMessage.class);

    public static final Type<GiveStarterKitMessage> TYPE = new Type<>(RavenDndOrigins.loc("give_starter_kit"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GiveStarterKitMessage> STREAM_CODEC =
            StreamCodec.unit(new GiveStarterKitMessage());

    public static void handle(GiveStarterKitMessage message, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) {
            return;
        }
        ctx.enqueueWork(() -> {
            if (RavenDndOriginsConfig.getInstance() == null) {
                LOGGER.warn("Config not loaded, cannot give starter kit");
                return;
            }

            StarterKitUtil.giveItemEntries(player, RavenDndOriginsConfig.starterKitItems());

            PlayerOriginsImpl state = PlayerOriginsAttachment.get(player);
            if (state == null) {
                LOGGER.warn("No origin state for player {}, skipping class starter kit entries", player.getName().getString());
                return;
            }

            ResourceLocation classLayerId = RavenDndOrigins.loc("class");
            if (!state.hasOrigin(classLayerId)) {
                LOGGER.debug("Player {} has no class origin on {}, skipping class starter kit entries",
                        player.getName().getString(), classLayerId);
                return;
            }

            ResourceLocation resolvedClassOrigin = state.getOrigin(classLayerId);
            List<? extends String> classEntries = RavenDndOriginsConfig.classStarterKitEntries();
            for (String row : classEntries) {
                String[] parts = row.split("\\|", 4);
                if (parts.length < 3) {
                    LOGGER.warn("Invalid class starter kit entry (need at least origin|item|count): {}", row);
                    continue;
                }
                ResourceLocation entryOriginId;
                try {
                    entryOriginId = ResourceLocation.parse(parts[0].trim());
                } catch (Exception e) {
                    LOGGER.warn("Invalid class origin id in class starter kit entry: {}", row);
                    continue;
                }
                if (!entryOriginId.equals(resolvedClassOrigin)) {
                    continue;
                }
                String itemPart = parts[1].trim() + "|" + parts[2].trim() + "|";
                if (parts.length > 3) {
                    itemPart += parts[3];
                }
                StarterKitUtil.giveSingleItemEntry(player, itemPart);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
