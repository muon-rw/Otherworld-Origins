package dev.muon.otherworldorigins.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Parses {@code item_id|count|nbt} entries and gives stacks to a player.
 */
public final class StarterKitUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(StarterKitUtil.class);

    private StarterKitUtil() {}

    public static void giveItemEntries(ServerPlayer player, List<? extends String> entries) {
        for (String itemEntry : entries) {
            giveSingleItemEntry(player, itemEntry);
        }
    }

    public static void giveSingleItemEntry(ServerPlayer player, String itemEntry) {
        try {
            String[] parts = itemEntry.split("\\|", 3);
            if (parts.length < 2) {
                LOGGER.warn("Invalid starter kit entry format: {}", itemEntry);
                return;
            }

            String itemIdStr = parts[0].trim();
            String countStr = parts[1].trim();
            String nbtStr = parts.length > 2 ? parts[2].trim() : "";

            ResourceLocation itemId = ResourceLocation.parse(itemIdStr);
            Item item = ForgeRegistries.ITEMS.getValue(itemId);
            if (item == null) {
                LOGGER.warn("Unknown item ID in starter kit: {}", itemIdStr);
                return;
            }

            int count;
            try {
                count = Integer.parseInt(countStr);
                if (count <= 0) {
                    LOGGER.warn("Invalid count in starter kit entry: {}", itemEntry);
                    return;
                }
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid count format in starter kit entry: {}", itemEntry);
                return;
            }

            ItemStack stack = new ItemStack(item, count);

            if (!nbtStr.isEmpty()) {
                try {
                    CompoundTag nbt = TagParser.parseTag(nbtStr);
                    stack.setTag(nbt);
                } catch (Exception e) {
                    LOGGER.warn("Failed to parse NBT for starter kit item {}: {}", itemIdStr, e.getMessage());
                }
            }

            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        } catch (Exception e) {
            LOGGER.error("Error processing starter kit entry: {}", itemEntry, e);
        }
    }
}
