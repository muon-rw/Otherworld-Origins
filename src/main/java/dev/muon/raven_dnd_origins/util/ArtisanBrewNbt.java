package dev.muon.raven_dnd_origins.util;

import dev.muon.raven_dnd_origins.component.ModDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Alchemist artificer potion bonuses. {@link #write} records the bonus in the
 * {@code raven_dnd_origins:artisan_brew} component and bakes a boosted copy of each base effect into
 * the stack's {@code minecraft:potion_contents} custom effects, leaving the potion holder (and so the
 * item name, colour and further brewing) untouched. {@code PotionContentsMixin} collapses the resulting
 * duplicate effect entries down to the strongest one.
 */
public final class ArtisanBrewNbt {

    private ArtisanBrewNbt() {
    }

    public static boolean has(ItemStack stack) {
        return stack.has(ModDataComponents.ARTISAN_BREW.get());
    }

    @Nullable
    public static ModDataComponents.ArtisanBrew get(ItemStack stack) {
        return stack.get(ModDataComponents.ARTISAN_BREW.get());
    }

    public static Bonus getBonus(ItemStack stack, MobEffectCategory category) {
        ModDataComponents.ArtisanBrew brew = get(stack);
        if (brew == null) {
            return Bonus.NONE;
        }
        return switch (category) {
            case BENEFICIAL -> brew.beneficial();
            case HARMFUL -> brew.harmful();
            case NEUTRAL -> brew.neutral();
        };
    }

    public static void write(ItemStack stack, @Nullable UUID brewerUuid, Bonus beneficial, Bonus harmful, Bonus neutral) {
        if (beneficial.isNone() && harmful.isNone() && neutral.isNone()) {
            return;
        }
        stack.set(ModDataComponents.ARTISAN_BREW.get(),
                new ModDataComponents.ArtisanBrew(Optional.ofNullable(brewerUuid), beneficial, harmful, neutral));
        boostPotionContents(stack);
    }

    private static void boostPotionContents(ItemStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) {
            return;
        }
        List<MobEffectInstance> boosted = new ArrayList<>();
        for (MobEffectInstance effect : contents.getAllEffects()) {
            Bonus bonus = getBonus(stack, effect.getEffect().value().getCategory());
            if (bonus.isNone()) {
                continue;
            }
            int duration = effect.getEffect().value().isInstantenous()
                    ? effect.getDuration()
                    : Math.round(effect.getDuration() * bonus.durationMultiplier());
            int amplifier = Math.max(0, effect.getAmplifier() + bonus.amplifierAdd());
            boosted.add(new MobEffectInstance(effect.getEffect(), duration, amplifier,
                    effect.isAmbient(), effect.isVisible(), effect.showIcon()));
        }
        if (boosted.isEmpty()) {
            return;
        }
        stack.set(DataComponents.POTION_CONTENTS,
                new PotionContents(contents.potion(), contents.customColor(), boosted));
    }

    public record Bonus(float durationMultiplier, int amplifierAdd) {
        public static final Bonus NONE = new Bonus(1.0f, 0);

        public boolean isNone() {
            return durationMultiplier == 1.0f && amplifierAdd == 0;
        }

        public Bonus combine(Bonus other) {
            return new Bonus(durationMultiplier * other.durationMultiplier, amplifierAdd + other.amplifierAdd);
        }
    }
}
