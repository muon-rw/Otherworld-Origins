package dev.muon.raven_dnd_origins.util;

import com.mojang.serialization.Dynamic;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Parses {@code item_id|count|nbt} entries and gives stacks to a player. The third field is applied as
 * real item data: either a 1.21 component patch ({@code {"minecraft:enchantments":{...}}}) or pre-1.20.5
 * item NBT ({@code {Enchantments:[...]}}), which is run through vanilla's item stack datafixer first.
 */
public final class StarterKitUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(StarterKitUtil.class);
    private static final int LEGACY_NBT_DATA_VERSION = 3465; // 1.20.1

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
            Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
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
                applyConfiguredData(stack, player.registryAccess(), itemIdStr, nbtStr);
            }

            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        } catch (Exception e) {
            LOGGER.error("Error processing starter kit entry: {}", itemEntry, e);
        }
    }

    private static void applyConfiguredData(ItemStack stack, HolderLookup.Provider registries, String itemIdStr, String nbtStr) {
        CompoundTag nbt;
        try {
            nbt = TagParser.parseTag(nbtStr);
        } catch (Exception e) {
            LOGGER.warn("Failed to parse NBT for starter kit item {}: {}", itemIdStr, e.getMessage());
            return;
        }

        Optional<DataComponentPatch> patch = DataComponentPatch.CODEC
                .parse(registries.createSerializationContext(NbtOps.INSTANCE), nbt)
                .result()
                .or(() -> upgradeLegacyTag(registries, itemIdStr, nbt));
        if (patch.isPresent()) {
            stack.applyComponents(patch.get());
        } else {
            LOGGER.warn("Starter kit NBT for {} is neither a component patch nor convertible item NBT: {}", itemIdStr, nbtStr);
        }
    }

    private static Optional<DataComponentPatch> upgradeLegacyTag(HolderLookup.Provider registries, String itemIdStr, CompoundTag nbt) {
        CompoundTag legacyStack = new CompoundTag();
        legacyStack.putString("id", itemIdStr);
        legacyStack.putByte("Count", (byte) 1);
        legacyStack.put("tag", nbt);
        try {
            Dynamic<Tag> fixed = DataFixers.getDataFixer().update(References.ITEM_STACK,
                    new Dynamic<>(NbtOps.INSTANCE, legacyStack),
                    LEGACY_NBT_DATA_VERSION,
                    SharedConstants.getCurrentVersion().getDataVersion().getVersion());
            return ItemStack.parse(registries, fixed.getValue()).map(ItemStack::getComponentsPatch);
        } catch (Exception e) {
            LOGGER.warn("Failed to convert legacy NBT for starter kit item {}: {}", itemIdStr, e.getMessage());
            return Optional.empty();
        }
    }
}
