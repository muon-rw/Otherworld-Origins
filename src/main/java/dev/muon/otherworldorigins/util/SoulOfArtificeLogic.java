package dev.muon.otherworldorigins.util;

import dev.shadowsoffire.apotheosis.adventure.affix.Affix;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixRegistry;
import dev.shadowsoffire.apotheosis.adventure.affix.AffixType;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.loot.LootController;
import dev.shadowsoffire.apotheosis.adventure.loot.LootRarity;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Shared logic for {@link dev.muon.otherworldorigins.action.item.SoulOfArtificeItemAction} and the affixable item condition.
 */
public final class SoulOfArtificeLogic {

    private static final Random NAME_SHUFFLE = new Random();

    private SoulOfArtificeLogic() {
    }

    /**
     * Whether {@link #applyOnItem} would add a bonus affix (matches the roll after stripping any existing soul affix).
     */
    public static boolean canApplyBonusAffix(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (AffixRegistry.INSTANCE.getValues().isEmpty()) {
            return false;
        }
        if (LootCategory.forItem(stack).isNone()) {
            return false;
        }
        var rarityHolder = AffixHelper.getRarity(stack);
        if (!rarityHolder.isBound()) {
            return false;
        }
        LootRarity rarity = rarityHolder.get();
        Set<DynamicHolder<? extends Affix>> current = new HashSet<>(AffixHelper.getAffixes(stack).keySet());
        subtractSoulAffixFromCurrent(stack, current);
        return anyAvailableAffixForRarity(stack, rarity, current);
    }

    private static void subtractSoulAffixFromCurrent(ItemStack stack, Set<DynamicHolder<? extends Affix>> current) {
        String soulId = SoulOfArtificeNbt.getAffixIdString(stack);
        if (soulId == null) {
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(soulId);
        if (id != null) {
            current.removeIf(h -> h.getId().equals(id));
        }
    }

    private static boolean anyAvailableAffixForRarity(ItemStack stack, LootRarity rarity, Set<DynamicHolder<? extends Affix>> current) {
        for (LootRarity.LootRule rule : rarity.getRules()) {
            AffixType type = rule.type();
            if (type == AffixType.DURABILITY || type == AffixType.SOCKET) {
                continue;
            }
            if (!LootController.getAvailableAffixes(stack, rarity, current, type).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public static void applyOnItem(Level level, ItemStack stack) {
        if (level.isClientSide) {
            return;
        }
        if (stack.isEmpty()) {
            return;
        }
        if (LootCategory.forItem(stack).isNone()) {
            return;
        }
        var rarityHolder = AffixHelper.getRarity(stack);
        if (!rarityHolder.isBound()) {
            return;
        }
        LootRarity rarity = rarityHolder.get();

        String prevId = SoulOfArtificeNbt.getAffixIdString(stack);
        if (prevId != null) {
            ResourceLocation prevLoc = ResourceLocation.tryParse(prevId);
            if (prevLoc != null) {
                Map<DynamicHolder<? extends Affix>, AffixInstance> map =
                        new HashMap<>(AffixHelper.getAffixes(stack));
                map.entrySet().removeIf(e -> e.getKey().getId().equals(prevLoc));
                AffixHelper.setAffixes(stack, map);
            }
        }
        SoulOfArtificeNbt.clearActive(stack);

        Set<DynamicHolder<? extends Affix>> current =
                new HashSet<>(AffixHelper.getAffixes(stack).keySet());

        List<LootRarity.LootRule> rules = new ArrayList<>(rarity.getRules());
        Collections.shuffle(rules, new Random(level.random.nextLong()));

        for (LootRarity.LootRule rule : rules) {
            AffixType type = rule.type();
            if (type == AffixType.DURABILITY || type == AffixType.SOCKET) {
                continue;
            }
            List<DynamicHolder<? extends Affix>> available =
                    LootController.getAvailableAffixes(stack, rarity, current, type);
            if (available.isEmpty()) {
                continue;
            }
            Collections.shuffle(available, new Random(level.random.nextLong()));
            DynamicHolder<? extends Affix> chosen = available.get(0);
            AffixHelper.applyAffix(stack, new AffixInstance(chosen, stack, rarityHolder, level.random.nextFloat()));
            SoulOfArtificeNbt.setActive(stack, chosen.getId().toString());
            refreshAffixName(stack, rarity, level.random);
            return;
        }
    }

    private static void refreshAffixName(ItemStack stack, LootRarity rarity, RandomSource rand) {
        List<AffixInstance> nameList = AffixHelper.streamAffixes(stack).collect(Collectors.toCollection(ArrayList::new));
        if (nameList.isEmpty()) {
            return;
        }
        NAME_SHUFFLE.setSeed(rand.nextLong());
        Collections.shuffle(nameList, NAME_SHUFFLE);
        String key = nameList.size() > 1 ? "misc.apotheosis.affix_name.three" : "misc.apotheosis.affix_name.two";
        MutableComponent name = Component.translatable(
                key,
                nameList.get(0).getName(true),
                "",
                nameList.size() > 1 ? nameList.get(1).getName(false) : ""
        ).withStyle(Style.EMPTY.withColor(rarity.getColor()));
        AffixHelper.setName(stack, name);
    }
}
