package dev.muon.otherworldorigins.util;

import dev.muon.otherworldorigins.compat.elixirum.ArsElixirHelper;
import dev.obscuria.elixirum.common.alchemy.registry.EssenceHolder;
import dev.obscuria.elixirum.common.alchemy.traits.Focus;
import dev.obscuria.elixirum.common.alchemy.traits.Form;
import dev.obscuria.elixirum.common.alchemy.traits.Risk;
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
     * Consumes one reagent from the main hand and gives the player a brewed result. The
     * concrete result depends on {@link ExperimentalElixirConfiguration.Configuration#outputMode()}:
     * vanilla potion (default) or a quality-scaled Ars Elixirum elixir. Server-side only.
     */
    public static void brewForPlayer(Player player, ExperimentalElixirConfiguration.Configuration configuration, RandomSource random) {
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!isReagent(stack)) {
            return;
        }

        boolean produced = switch (configuration.outputMode()) {
            case VANILLA_POTION -> brewVanillaPotion(player, configuration, random);
            case ARS_ELIXIR -> brewArsElixir(player, configuration, random);
        };

        if (produced && configuration.masteryXp() > 0) {
            ArsElixirHelper.grantMasteryXp(player, configuration.masteryXp());
        }
    }

    /**
     * Picks essence holders from the Ars codex matching the configured category, builds an
     * elixir, and gives it to the player. If the codex has no matching essences (e.g. pack
     * config strips a category), falls back to the vanilla potion path so the charge is never
     * spent for nothing.
     */
    private static boolean brewArsElixir(Player player, ExperimentalElixirConfiguration.Configuration configuration, RandomSource random) {
        int count = rollCount(configuration, random);
        List<EssenceHolder> essences = ArsElixirHelper.pickEssencesForCategory(player.level(), configuration.category(), count, random);
        if (essences.isEmpty()) {
            return brewVanillaPotion(player, configuration, random);
        }

        int amplifier = configuration.amplifierMin().isPresent()
                ? Mth.nextInt(random, configuration.amplifierMin().get(), configuration.amplifierMax().get())
                : 0;
        int duration = configuration.durationMin().isPresent()
                ? Mth.nextInt(random, configuration.durationMin().get(), configuration.durationMax().get())
                : 600;

        ItemStack elixir = ArsElixirHelper.buildElixirStack(
                player,
                essences,
                amplifier,
                duration,
                Risk.PERFECT,
                Focus.BALANCED,
                configuration.lingering() ? Form.LINGERING : Form.POTABLE
        );

        consumeReagent(player, stack(player));
        giveOrDrop(player, elixir);
        return true;
    }

    private static boolean brewVanillaPotion(Player player, ExperimentalElixirConfiguration.Configuration configuration, RandomSource random) {
        List<Potion> pool = potionsMatching(configuration.category());
        if (pool.isEmpty()) {
            return false;
        }

        List<MobEffectInstance> adjusted = buildAdjustedEffects(pool, configuration, random);

        ItemStack elixir = new ItemStack(configuration.lingering() ? Items.LINGERING_POTION : Items.POTION);
        CompoundTag tag = elixir.getOrCreateTag();
        tag.remove("CustomPotionEffects");
        tag.remove("CustomPotionColor");
        PotionUtils.setPotion(elixir, Potions.WATER);
        PotionUtils.setCustomEffects(elixir, adjusted);
        elixir.setHoverName(Component.translatable("item.otherworldorigins.experimental_elixir").withStyle(ChatFormatting.LIGHT_PURPLE));

        consumeReagent(player, stack(player));
        giveOrDrop(player, elixir);
        return true;
    }

    private static ItemStack stack(Player player) {
        return player.getItemInHand(InteractionHand.MAIN_HAND);
    }

    private static void consumeReagent(Player player, ItemStack stack) {
        stack.shrink(1);
        if (stack.isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
    }

    private static void giveOrDrop(Player player, ItemStack elixir) {
        if (!player.addItem(elixir)) {
            player.drop(elixir, false);
        }
    }

    private static int rollCount(ExperimentalElixirConfiguration.Configuration configuration, RandomSource random) {
        int min = configuration.effectCountMin().orElse(1);
        int max = configuration.effectCountMax().orElse(1);
        return Mth.nextInt(random, min, max);
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
        int count = rollCount(configuration, random);

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
