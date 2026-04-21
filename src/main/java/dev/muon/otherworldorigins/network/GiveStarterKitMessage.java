package dev.muon.otherworldorigins.network;

import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.config.OtherworldOriginsConfig;
import dev.muon.otherworldorigins.util.StarterKitUtil;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Supplier;

public class GiveStarterKitMessage {
    private static final Logger LOGGER = LoggerFactory.getLogger(GiveStarterKitMessage.class);
    private static final ResourceLocation EMPTY_ORIGIN = ResourceLocation.fromNamespaceAndPath("origins", "empty");

    public GiveStarterKitMessage() {}

    public static GiveStarterKitMessage decode(FriendlyByteBuf buf) {
        return new GiveStarterKitMessage();
    }

    public void encode(FriendlyByteBuf buf) {}

    public static void handle(GiveStarterKitMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            if (OtherworldOriginsConfig.getInstance() == null) {
                LOGGER.warn("Config not loaded, cannot give starter kit");
                return;
            }

            StarterKitUtil.giveItemEntries(player, OtherworldOriginsConfig.starterKitItems());

            IOriginContainer originContainer = IOriginContainer.get(player).resolve().orElse(null);
            if (originContainer == null) {
                LOGGER.warn("No origin container for player {}, skipping class starter kit entries", player.getName().getString());
                return;
            }

            ResourceLocation classLayerId = OtherworldOrigins.loc("class");
            ResourceKey<Origin> classOriginKey = originContainer.getOrigin(
                    ResourceKey.create(OriginsAPI.getLayersRegistry(null).key(), classLayerId));

            if (classOriginKey == null || EMPTY_ORIGIN.equals(classOriginKey.location())) {
                LOGGER.debug("Player {} has no class origin on {}, skipping class starter kit entries",
                        player.getName().getString(), classLayerId);
                return;
            }

            ResourceLocation resolvedClassOrigin = classOriginKey.location();
            List<? extends String> classEntries = OtherworldOriginsConfig.classStarterKitEntries();
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
        ctx.get().setPacketHandled(true);
    }
}
