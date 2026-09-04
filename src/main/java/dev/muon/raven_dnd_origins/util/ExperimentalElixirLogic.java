package dev.muon.raven_dnd_origins.util;

import dev.muon.raven_dnd_origins.action.entity.ExperimentalElixirBrewAction;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Brews the Alchemist's Experimental Elixir: a vanilla potion carrying effects rolled from the
 * registered potions of one category. Effects are written onto the item as potion contents, so the
 * roll happens once per elixir and the drink path creates fresh effect instances; nothing here
 * touches durations of effects already on an entity.
 */
public final class ExperimentalElixirLogic {
    private ExperimentalElixirLogic() {}

    public static boolean isReagent(ItemStack stack) {
        if (stack.is(Items.GLASS_BOTTLE)) {
            return true;
        }
        if (!stack.is(Items.POTION)) {
            return false;
        }
        PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        return contents.is(Potions.WATER) || contents.is(Potions.AWKWARD);
    }

    public static void brewForPlayer(Player player, ExperimentalElixirBrewAction.Cfg cfg, RandomSource random) {
        ItemStack reagent = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!isReagent(reagent)) {
            return;
        }
        List<Potion> pool = potionsMatching(cfg.category());
        List<MobEffectInstance> effects = rollEffects(pool, cfg, random);
        if (effects.isEmpty()) {
            return;
        }
        ItemStack elixir = new ItemStack(cfg.lingering() ? Items.LINGERING_POTION : Items.POTION);
        // No base potion: a water base would make the elixir a brewing-stand input (nether wart turns
        // it into a plain awkward potion) and a reagent for the next brew.
        elixir.set(DataComponents.POTION_CONTENTS,
                new PotionContents(Optional.empty(), Optional.empty(), effects));
        elixir.set(DataComponents.CUSTOM_NAME,
                Component.translatable("item.raven_dnd_origins.experimental_elixir").withStyle(ChatFormatting.LIGHT_PURPLE));
        reagent.shrink(1);
        if (!player.addItem(elixir)) {
            player.drop(elixir, false);
        }
    }

    /**
     * Rolls {@code effect_count_min}..{@code effect_count_max} effects, each the primary effect of a
     * random potion in the pool, preferring effect types not yet rolled while any remain.
     */
    private static List<MobEffectInstance> rollEffects(List<Potion> pool, ExperimentalElixirBrewAction.Cfg cfg, RandomSource random) {
        List<Potion> usable = new ArrayList<>();
        for (Potion potion : pool) {
            if (!potion.getEffects().isEmpty()) {
                usable.add(potion);
            }
        }
        if (usable.isEmpty()) {
            return List.of();
        }
        int count = Mth.nextInt(random, cfg.effectCountMin().orElse(1), cfg.effectCountMax().orElse(1));
        List<MobEffectInstance> result = new ArrayList<>(count);
        List<MobEffect> usedPrimaries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            List<Potion> unused = new ArrayList<>();
            for (Potion candidate : usable) {
                if (!usedPrimaries.contains(primaryOf(candidate).value())) {
                    unused.add(candidate);
                }
            }
            List<Potion> candidates = unused.isEmpty() ? usable : unused;
            MobEffectInstance template = candidates.get(random.nextInt(candidates.size())).getEffects().getFirst();
            usedPrimaries.add(template.getEffect().value());
            result.add(adjust(template, cfg, random));
        }
        return result;
    }

    private static Holder<MobEffect> primaryOf(Potion potion) {
        return potion.getEffects().getFirst().getEffect();
    }

    private static List<Potion> potionsMatching(MobEffectCategory category) {
        List<Potion> list = new ArrayList<>();
        for (Potion potion : BuiltInRegistries.POTION) {
            if (allEffectsIn(potion, category)) {
                list.add(potion);
            }
        }
        return list;
    }

    private static boolean allEffectsIn(Potion potion, MobEffectCategory category) {
        List<MobEffectInstance> effects = potion.getEffects();
        if (effects.isEmpty()) {
            return false;
        }
        for (MobEffectInstance instance : effects) {
            if (instance.getEffect().value().getCategory() != category) {
                return false;
            }
        }
        return true;
    }

    private static MobEffectInstance adjust(MobEffectInstance base, ExperimentalElixirBrewAction.Cfg cfg, RandomSource random) {
        int duration = base.getDuration();
        int amplifier = base.getAmplifier();
        if (cfg.durationMin().isPresent() && cfg.durationMax().isPresent()) {
            duration = Mth.nextInt(random, cfg.durationMin().get(), cfg.durationMax().get());
        }
        if (cfg.amplifierMin().isPresent() && cfg.amplifierMax().isPresent()) {
            amplifier = Mth.nextInt(random, cfg.amplifierMin().get(), cfg.amplifierMax().get());
        }
        return new MobEffectInstance(base.getEffect(), duration, amplifier, base.isAmbient(), base.isVisible(), base.showIcon());
    }
}
