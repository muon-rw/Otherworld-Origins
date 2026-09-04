package dev.muon.raven_dnd_origins.util;

import dev.shadowsoffire.apotheosis.Apoth;
import dev.shadowsoffire.apotheosis.affix.Affix;
import dev.shadowsoffire.apotheosis.affix.AffixHelper;
import dev.shadowsoffire.apotheosis.affix.AffixInstance;
import dev.shadowsoffire.apotheosis.affix.AffixRegistry;
import dev.shadowsoffire.apotheosis.affix.AffixType;
import dev.shadowsoffire.apotheosis.affix.ItemAffixes;
import dev.shadowsoffire.apotheosis.loot.LootCategory;
import dev.shadowsoffire.apotheosis.loot.LootController;
import dev.shadowsoffire.apotheosis.loot.LootRarity;
import dev.shadowsoffire.apotheosis.loot.LootRule;
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
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Shared logic for {@link dev.muon.raven_dnd_origins.action.item.SoulOfArtificeItemAction} and the affixable item condition.
 */
public final class SoulOfArtificeLogic {

    private static final Random NAME_SHUFFLE = new Random();

    private SoulOfArtificeLogic() {
    }

    /** Whether {@link #applyOnItem} would add a bonus affix, ignoring any soul affix already rolled. */
    public static boolean canApplyBonusAffix(ItemStack stack) {
        if (stack.isEmpty() || AffixRegistry.INSTANCE.getValues().isEmpty()) {
            return false;
        }
        LootCategory cat = LootCategory.forItem(stack);
        if (cat.isNone()) {
            return false;
        }
        DynamicHolder<LootRarity> rarityHolder = AffixHelper.getRarity(stack);
        if (!rarityHolder.isBound()) {
            return false;
        }
        ItemStack probe = withoutSoulAffix(stack);
        for (AffixType type : rollableAffixTypes(rarityHolder.get(), cat)) {
            if (LootController.getAvailableAffixes(probe, rarityHolder.get(), type).findAny().isPresent()) {
                return true;
            }
        }
        return false;
    }

    public static void applyOnItem(Level level, ItemStack stack) {
        if (level.isClientSide || stack.isEmpty()) {
            return;
        }
        LootCategory cat = LootCategory.forItem(stack);
        if (cat.isNone()) {
            return;
        }
        DynamicHolder<LootRarity> rarityHolder = AffixHelper.getRarity(stack);
        if (!rarityHolder.isBound()) {
            return;
        }
        LootRarity rarity = rarityHolder.get();

        AffixHelper.setAffixes(stack, affixesWithoutSoul(stack));
        SoulOfArtificeNbt.clearActive(stack);

        List<AffixType> types = rollableAffixTypes(rarity, cat);
        Collections.shuffle(types, new Random(level.random.nextLong()));

        for (AffixType type : types) {
            List<DynamicHolder<Affix>> available =
                    LootController.getAvailableAffixes(stack, rarity, type).collect(Collectors.toList());
            if (available.isEmpty()) {
                continue;
            }
            Collections.shuffle(available, new Random(level.random.nextLong()));
            DynamicHolder<Affix> chosen = available.getFirst();
            AffixHelper.applyAffix(stack, new AffixInstance(chosen, level.random.nextFloat(), rarityHolder, stack));
            SoulOfArtificeNbt.setActive(stack, chosen.getId().toString());
            refreshAffixName(stack, rarity, level.random);
            return;
        }
    }

    /** Only the types the rarity's own rules declare; LootController.getAvailableAffixes does not check the rule list. */
    private static List<AffixType> rollableAffixTypes(LootRarity rarity, LootCategory cat) {
        List<AffixType> types = new ArrayList<>();
        for (LootRule rule : rarity.getRules(cat)) {
            collectAffixTypes(rule, types);
        }
        return types;
    }

    private static void collectAffixTypes(LootRule rule, List<AffixType> out) {
        switch (rule) {
            case LootRule.AffixLootRule affix -> {
                if (!out.contains(affix.type())) {
                    out.add(affix.type());
                }
            }
            case LootRule.ChancedLootRule chanced -> collectAffixTypes(chanced.rule(), out);
            case LootRule.CombinedLootRule combined -> combined.rules().forEach(r -> collectAffixTypes(r, out));
            case LootRule.SelectLootRule select -> {
                collectAffixTypes(select.ifTrue(), out);
                collectAffixTypes(select.ifFalse(), out);
            }
            default -> {
            }
        }
    }

    private static ItemStack withoutSoulAffix(ItemStack stack) {
        ResourceLocation soulId = SoulOfArtificeNbt.getAffixId(stack);
        if (soulId == null) {
            return stack;
        }
        ItemStack copy = stack.copy();
        AffixHelper.setAffixes(copy, affixesWithoutSoul(stack));
        return copy;
    }

    private static ItemAffixes affixesWithoutSoul(ItemStack stack) {
        ItemAffixes.Builder builder = stack.getOrDefault(Apoth.Components.AFFIXES, ItemAffixes.EMPTY).toBuilder();
        ResourceLocation soulId = SoulOfArtificeNbt.getAffixId(stack);
        if (soulId != null) {
            builder.removeIf(a -> a.getId().equals(soulId));
        }
        return builder.build();
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
        ).withStyle(Style.EMPTY.withColor(rarity.color()));
        AffixHelper.setName(stack, name);
    }
}
