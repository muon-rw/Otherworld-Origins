package dev.muon.otherworldorigins.network;

import dev.muon.otherworldorigins.config.OtherworldOriginsConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Supplier;

public class GiveStarterKitMessage {
    private static final Logger LOGGER = LoggerFactory.getLogger(GiveStarterKitMessage.class);

    public GiveStarterKitMessage() {}

    public static GiveStarterKitMessage decode(FriendlyByteBuf buf) {
        return new GiveStarterKitMessage();
    }

    public void encode(FriendlyByteBuf buf) {}

    public static void handle(GiveStarterKitMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                if (!OtherworldOriginsConfig.SPEC.isLoaded()) {
                    LOGGER.warn("Config not loaded, cannot give starter kit");
                    return;
                }

                List<? extends String> starterKitItems = OtherworldOriginsConfig.STARTER_KIT_ITEMS.get();
                
                for (String itemEntry : starterKitItems) {
                    try {
                        String[] parts = itemEntry.split("\\|", 3);
                        if (parts.length < 2) {
                            LOGGER.warn("Invalid starter kit entry format: {}", itemEntry);
                            continue;
                        }

                        String itemIdStr = parts[0].trim();
                        String countStr = parts[1].trim();
                        String nbtStr = parts.length > 2 ? parts[2].trim() : "";

                        // Parse item ID
                        ResourceLocation itemId = ResourceLocation.parse(itemIdStr);
                        Item item = ForgeRegistries.ITEMS.getValue(itemId);
                        if (item == null) {
                            LOGGER.warn("Unknown item ID in starter kit: {}", itemIdStr);
                            continue;
                        }

                        // Parse count
                        int count;
                        try {
                            count = Integer.parseInt(countStr);
                            if (count <= 0) {
                                LOGGER.warn("Invalid count in starter kit entry: {}", itemEntry);
                                continue;
                            }
                        } catch (NumberFormatException e) {
                            LOGGER.warn("Invalid count format in starter kit entry: {}", itemEntry);
                            continue;
                        }

                        // Create item stack
                        ItemStack stack = new ItemStack(item, count);

                        // Parse and apply NBT if provided
                        if (!nbtStr.isEmpty()) {
                            try {
                                CompoundTag nbt = TagParser.parseTag(nbtStr);
                                stack.setTag(nbt);
                            } catch (Exception e) {
                                LOGGER.warn("Failed to parse NBT for starter kit item {}: {}", itemIdStr, e.getMessage());
                            }
                        }

                        // Give item to player
                        if (!player.getInventory().add(stack)) {
                            // If inventory is full, drop the item
                            player.drop(stack, false);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error processing starter kit entry: {}", itemEntry, e);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
