package dev.muon.otherworldorigins.util;

import dev.muon.otherworldorigins.OtherworldOrigins;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Tracks Apotheosis "Soul of Artifice" bonus affix state under the mod root tag.
 */
public final class SoulOfArtificeNbt {

    public static final String SOUL_FLAG = "soul_of_artifice";
    public static final String SOUL_AFFIX_ID = "soul_of_artifice_affix";

    private SoulOfArtificeNbt() {
    }

    public static boolean isActive(ItemStack stack) {
        CompoundTag root = stack.getTagElement(OtherworldOrigins.MODID);
        return root != null && root.getBoolean(SOUL_FLAG);
    }

    @Nullable
    public static String getAffixIdString(ItemStack stack) {
        CompoundTag root = stack.getTagElement(OtherworldOrigins.MODID);
        if (root == null || !root.contains(SOUL_AFFIX_ID, Tag.TAG_STRING)) {
            return null;
        }
        return root.getString(SOUL_AFFIX_ID);
    }

    public static void setActive(ItemStack stack, String affixIdString) {
        CompoundTag root = stack.getOrCreateTagElement(OtherworldOrigins.MODID);
        root.putBoolean(SOUL_FLAG, true);
        root.putString(SOUL_AFFIX_ID, affixIdString);
    }

    /**
     * Clears the marker (boolean 0) and affix id; removes an empty mod root tag.
     */
    public static void clearActive(ItemStack stack) {
        if (!stack.hasTag()) {
            return;
        }
        CompoundTag tag = stack.getTag();
        if (!tag.contains(OtherworldOrigins.MODID, Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag root = tag.getCompound(OtherworldOrigins.MODID);
        root.putBoolean(SOUL_FLAG, false);
        root.remove(SOUL_AFFIX_ID);
        if (root.isEmpty()) {
            tag.remove(OtherworldOrigins.MODID);
        }
        if (tag.isEmpty()) {
            stack.setTag(null);
        }
    }
}
