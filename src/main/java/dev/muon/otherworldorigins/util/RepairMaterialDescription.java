package dev.muon.otherworldorigins.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Data for shift-expandable repair tooltips: collapsed hint vs expanded header + ingredient list.
 */
public final class RepairMaterialDescription {
    private static final int MAX_DISCOVERED = 24;
    private static final Map<Item, Optional<Component>> MATERIAL_CACHE = new HashMap<>();

    private RepairMaterialDescription() {}

    /** Call when tags reload (e.g. {@code /reload}) so cached material text matches current tags. */
    public static void invalidate() {
        synchronized (MATERIAL_CACHE) {
            MATERIAL_CACHE.clear();
        }
    }

    /** Durable items get a repair tooltip (collapsed or expanded on client). */
    public static boolean shouldShow(ItemStack stack) {
        return stack.isDamageableItem();
    }

    /** Single dark gray italic line when Shift is not held. */
    public static Component collapsedLine() {
        return Component.translatable("otherworldorigins.tooltip.repair_materials_hold_shift")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);
    }

    /**
     * Expanded block: gray non-italic header, then ingredient names (or {@code Not repairable}, or
     * same-item merge name) on the following line.
     */
    public static List<Component> expandedLines(ItemStack stack) {
        List<Component> lines = new ArrayList<>(2);
        lines.add(Component.translatable("otherworldorigins.tooltip.repair_materials_header")
                .withStyle(ChatFormatting.GRAY));
        if (!stack.isRepairable()) {
            lines.add(Component.translatable("otherworldorigins.tooltip.not_repairable").copy());
        } else {
            Optional<Component> material = materialDescription(stack);
            if (material.isPresent()) {
                lines.add(material.get().copy());
            } else {
                lines.add(stack.getHoverName().copy());
            }
        }
        return lines;
    }

    /** Display name(s) for anvil material repair only (second slot), when any exist. */
    private static Optional<Component> materialDescription(ItemStack stack) {
        Item item = stack.getItem();
        synchronized (MATERIAL_CACHE) {
            return MATERIAL_CACHE.computeIfAbsent(item, k -> resolveMaterial(stack).map(Component::copy));
        }
    }

    private static Optional<Component> resolveMaterial(ItemStack stack) {
        Item item = stack.getItem();
        Ingredient directIngredient = getDirectIngredient(item);
        if (directIngredient != null && !directIngredient.isEmpty()) {
            ItemStack[] stacks = directIngredient.getItems();
            if (stacks.length > 0) {
                ItemStack probe = new ItemStack(stacks[0].getItem());
                if (item.isValidRepairItem(stack, probe)) {
                    return formatIngredient(directIngredient);
                }
            }
        }
        return discover(stack);
    }

    @Nullable
    private static Ingredient getDirectIngredient(Item item) {
        if (item instanceof TieredItem tiered) {
            return tiered.getTier().getRepairIngredient();
        }
        if (item instanceof ArmorItem armor) {
            return armor.getMaterial().getRepairIngredient();
        }
        if (item instanceof ShieldItem) {
            return Ingredient.of(ItemTags.PLANKS);
        }
        return null;
    }

    private static Optional<Component> discover(ItemStack toRepair) {
        Item toRepairItem = toRepair.getItem();
        List<Component> names = new ArrayList<>();
        for (Item candidate : ForgeRegistries.ITEMS.getValues()) {
            if (candidate == toRepairItem) {
                continue;
            }
            ItemStack repair = new ItemStack(candidate);
            if (repair.isEmpty()) {
                continue;
            }
            if (toRepairItem.isValidRepairItem(toRepair, repair)) {
                names.add(repair.getHoverName());
                if (names.size() >= MAX_DISCOVERED) {
                    break;
                }
            }
        }
        if (names.isEmpty()) {
            return Optional.empty();
        }
        if (names.size() == 1) {
            return Optional.of(names.get(0));
        }
        MutableComponent joined = Component.literal("");
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) {
                joined.append(Component.literal(", "));
            }
            joined.append(names.get(i));
        }
        return Optional.of(joined);
    }

    private static Optional<Component> formatIngredient(Ingredient ingredient) {
        if (ingredient.isEmpty()) {
            return Optional.empty();
        }
        ItemStack[] stacks = ingredient.getItems();
        if (stacks.length == 0) {
            return Optional.empty();
        }
        if (stacks.length == 1) {
            return Optional.of(stacks[0].getHoverName());
        }
        MutableComponent joined = Component.literal("");
        int limit = Math.min(stacks.length, MAX_DISCOVERED);
        for (int i = 0; i < limit; i++) {
            if (i > 0) {
                joined.append(Component.literal(", "));
            }
            joined.append(stacks[i].getHoverName());
        }
        if (stacks.length > limit) {
            joined.append(Component.literal(", …"));
        }
        return Optional.of(joined);
    }
}
