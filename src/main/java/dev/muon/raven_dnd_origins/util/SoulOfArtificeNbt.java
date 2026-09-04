package dev.muon.raven_dnd_origins.util;

import dev.muon.raven_dnd_origins.component.ModDataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Tracks which bonus affix "Soul of Artifice" rolled onto an item. Presence of the component is the
 * active marker; the value is the rolled affix id.
 */
public final class SoulOfArtificeNbt {

    private SoulOfArtificeNbt() {
    }

    public static boolean isActive(ItemStack stack) {
        return stack.has(ModDataComponents.SOUL_OF_ARTIFICE.get());
    }

    @Nullable
    public static ResourceLocation getAffixId(ItemStack stack) {
        return stack.get(ModDataComponents.SOUL_OF_ARTIFICE.get());
    }

    @Nullable
    public static String getAffixIdString(ItemStack stack) {
        ResourceLocation id = getAffixId(stack);
        return id == null ? null : id.toString();
    }

    public static void setActive(ItemStack stack, String affixIdString) {
        ResourceLocation id = ResourceLocation.tryParse(affixIdString);
        if (id != null) {
            stack.set(ModDataComponents.SOUL_OF_ARTIFICE.get(), id);
        }
    }

    public static void clearActive(ItemStack stack) {
        stack.remove(ModDataComponents.SOUL_OF_ARTIFICE.get());
    }
}
