package dev.muon.otherworldorigins.util;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared rules and brew logic for {@link dev.muon.otherworldorigins.action.entity.ExperimentalElixirEntityAction}.
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
        var potion = PotionUtils.getPotion(stack);
        return potion == Potions.WATER || potion == Potions.AWKWARD;
    }

    /**
     * Consumes one reagent from the main hand and gives one brewed potion. Safe to call from server-side entity actions.
     */
    public static void brewForPlayer(Player player, ExperimentalElixirConfiguration.Configuration configuration, RandomSource random) {
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!isReagent(stack)) {
            return;
        }

        List<Potion> pool = potionsMatching(configuration.category());
        if (pool.isEmpty()) {
            return;
        }

        List<MobEffectInstance> adjusted = buildAdjustedEffects(pool, configuration, random);

        ItemStack elixir = new ItemStack(configuration.lingering() ? Items.LINGERING_POTION : Items.POTION);
        CompoundTag tag = elixir.getOrCreateTag();
        tag.remove("CustomPotionEffects");
        tag.remove("CustomPotionColor");
        PotionUtils.setPotion(elixir, Potions.WATER);
        PotionUtils.setCustomEffects(elixir, adjusted);
        elixir.setHoverName(Component.translatable("item.otherworldorigins.experimental_elixir").withStyle(ChatFormatting.LIGHT_PURPLE));

        stack.shrink(1);
        if (stack.isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }

        if (!player.addItem(elixir)) {
            player.drop(elixir, false);
        }
    }

    /**
     * Rolls {@code effect_count_min}–{@code effect_count_max} primary effects from the category pool (inclusive),
     * preferring distinct MobEffect types while any unused primaries remain in the pool.
     */
    private static List<MobEffectInstance> buildAdjustedEffects(
            List<Potion> pool,
            ExperimentalElixirConfiguration.Configuration configuration,
            RandomSource random
    ) {
        int min = configuration.effectCountMin().orElse(1);
        int max = configuration.effectCountMax().orElse(1);
        int count = Mth.nextInt(random, min, max);

        List<Potion> usable = new ArrayList<>();
        for (Potion potion : pool) {
            if (!potion.getEffects().isEmpty()) {
                usable.add(potion);
            }
        }
        if (usable.isEmpty()) {
            return List.of();
        }

        List<MobEffectInstance> result = new ArrayList<>(count);
        List<MobEffect> usedPrimaries = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            List<Potion> distinctPool = new ArrayList<>();
            for (Potion candidate : usable) {
                MobEffect primary = candidate.getEffects().get(0).getEffect();
                if (!usedPrimaries.contains(primary)) {
                    distinctPool.add(candidate);
                }
            }
            List<Potion> candidates = distinctPool.isEmpty() ? usable : distinctPool;
            Potion pick = candidates.get(random.nextInt(candidates.size()));
            MobEffectInstance template = pick.getEffects().get(0);
            usedPrimaries.add(template.getEffect());
            result.add(adjustEffect(template, configuration, random));
        }
        return result;
    }

    private static List<Potion> potionsMatching(MobEffectCategory category) {
        List<Potion> list = new ArrayList<>();
        for (Potion potion : BuiltInRegistries.POTION) {
            if (matchesCategory(potion, category)) {
                list.add(potion);
            }
        }
        return list;
    }

    private static boolean matchesCategory(Potion potion, MobEffectCategory category) {
        List<MobEffectInstance> effects = potion.getEffects();
        if (effects.isEmpty()) {
            return false;
        }
        for (MobEffectInstance inst : effects) {
            if (inst.getEffect().getCategory() != category) {
                return false;
            }
        }
        return true;
    }

    private static MobEffectInstance adjustEffect(
            MobEffectInstance base,
            ExperimentalElixirConfiguration.Configuration configuration,
            RandomSource random
    ) {
        MobEffect effect = base.getEffect();
        int duration = base.getDuration();
        int amplifier = base.getAmplifier();

        boolean useDurationRange = configuration.durationMin().isPresent() && configuration.durationMax().isPresent();
        boolean useAmplifierRange = configuration.amplifierMin().isPresent() && configuration.amplifierMax().isPresent();

        if (useDurationRange) {
            duration = Mth.nextInt(random, configuration.durationMin().get(), configuration.durationMax().get());
        }

        if (useAmplifierRange) {
            amplifier = Mth.nextInt(random, configuration.amplifierMin().get(), configuration.amplifierMax().get());
        }

        return new MobEffectInstance(effect, duration, amplifier, base.isAmbient(), base.isVisible(), base.showIcon());
    }
}
