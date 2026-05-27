package dev.muon.otherworldorigins.compat.elixirum;

import dev.muon.otherworldorigins.power.ModifyBrewedPotionPower;
import dev.obscuria.elixirum.api.ArsElixirumAPI;
import dev.obscuria.elixirum.api.alchemy.EffectProvider;
import dev.obscuria.elixirum.api.alchemy.components.ElixirContents;
import dev.obscuria.elixirum.api.codex.Alchemy;
import dev.obscuria.elixirum.api.codex.AlchemyProfile;
import dev.obscuria.elixirum.common.alchemy.basics.Essence;
import dev.obscuria.elixirum.common.alchemy.codex.components.KnownIngredients;
import dev.obscuria.elixirum.common.alchemy.providers.DirectEffectProvider;
import dev.obscuria.elixirum.common.alchemy.registry.EssenceHolder;
import dev.obscuria.elixirum.common.alchemy.traits.Focus;
import dev.obscuria.elixirum.common.alchemy.traits.Form;
import dev.obscuria.elixirum.common.alchemy.traits.Risk;
import dev.obscuria.elixirum.common.registry.ElixirumItems;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * All Ars Elixirum API interactions for the Artificer Alchemist subclass live here. Direct API
 * imports — Ars Elixirum is a hard required dependency.
 */
public final class ArsElixirHelper {

    private ArsElixirHelper() {}

    /**
     * Picks {@code count} random {@link EssenceHolder}s from the world's {@link Alchemy} codex
     * whose bound essence produces an effect in the given vanilla category. Returns fewer than
     * {@code count} if the codex has fewer matching essences.
     */
    public static List<EssenceHolder> pickEssencesForCategory(Level level, MobEffectCategory category, int count, RandomSource random) {
        List<EssenceHolder> pool = new ArrayList<>();
        Alchemy.get(level).essences().streamHolders().forEach(holder -> {
            if (!holder.isBound()) return;
            Essence essence = holder.value();
            if (essence == null) return;
            MobEffect effect = essence.effect().value();
            if (effect == null) return;
            if (effect.getCategory() == category) pool.add(holder);
        });
        if (pool.isEmpty()) return List.of();
        Collections.shuffle(pool, new java.util.Random(random.nextLong()));
        return pool.subList(0, Math.min(count, pool.size()));
    }

    /**
     * Builds a fresh elixir {@link ItemStack} with the given essences and traits, applying any
     * {@link ModifyBrewedPotionPower} bonuses the player holds (per category). Each essence's
     * amplifier and duration are clamped against the essence's own max before AND after the
     * bonus is applied.
     */
    public static ItemStack buildElixirStack(Player player, List<EssenceHolder> holders,
                                              int amplifier, int duration,
                                              Risk risk, Focus focus, Form form) {
        List<EffectProvider> providers = new ArrayList<>(holders.size());
        for (EssenceHolder holder : holders) {
            Essence essence = holder.value();
            if (essence == null) continue;
            int clampedAmp = Mth.clamp(amplifier, 0, essence.maxAmplifier());
            int clampedDur = Mth.clamp(duration, 1, Math.max(1, essence.maxDuration()));
            providers.add(applyBrewBonuses(player, holder, essence, clampedAmp, clampedDur));
        }
        ElixirContents contents = ElixirContents.create(providers, focus, form, risk);
        ItemStack stack = new ItemStack(ElixirumItems.ELIXIR.get());
        ArsElixirumAPI.setElixirContents(stack, contents);
        return stack;
    }

    /**
     * Rebuilds the elixir on {@code stack} with all {@link ModifyBrewedPotionPower} bonuses the
     * given player holds applied to each effect. Existing focus/form/color are preserved; risk
     * is preserved unless {@code overrideRisk} is provided. No-op if the stack has no contents
     * or the player has no relevant powers and no risk override.
     */
    public static void restampElixir(Player player, ItemStack stack, Risk overrideRisk) {
        ElixirContents existing = ArsElixirumAPI.getElixirContents(stack);
        if (existing == null || existing.isEmpty()) return;

        List<ModifyBrewedPotionPower.Configuration> configs = ModifyBrewedPotionPower.getActiveConfigs(player);
        if (configs.isEmpty() && overrideRisk == null) return;

        List<EffectProvider> rebuilt = new ArrayList<>(existing.effects().size());
        for (EffectProvider provider : existing.effects()) {
            EssenceHolder holder = provider.holder();
            Essence essence = holder.value();
            if (essence == null) {
                rebuilt.add(provider);
                continue;
            }
            rebuilt.add(applyBrewBonusesFromConfigs(holder, essence, provider.amplifier(), provider.duration(), configs));
        }

        Risk finalRisk = overrideRisk != null ? overrideRisk : existing.risk();
        ElixirContents updated = ElixirContents.create(rebuilt, existing.focus(), existing.form(), finalRisk, existing.color());
        ArsElixirumAPI.setElixirContents(stack, updated);
    }

    /**
     * Returns a {@link DirectEffectProvider} with amplifier/duration scaled by all of the
     * player's {@link ModifyBrewedPotionPower} configs that apply to the essence's effect
     * category. Both values are clamped to the essence's max.
     */
    private static EffectProvider applyBrewBonuses(Player player, EssenceHolder holder, Essence essence, int baseAmplifier, int baseDuration) {
        return applyBrewBonusesFromConfigs(holder, essence, baseAmplifier, baseDuration, ModifyBrewedPotionPower.getActiveConfigs(player));
    }

    /**
     * Bonus modifiers from {@link ModifyBrewedPotionPower} are intentionally NOT capped at
     * {@code essence.maxAmplifier()} / {@code essence.maxDuration()} — they exceed normal
     * crafting limits by design. Restoration grants supernormal amplitude on beneficial
     * elixirs the same way the vanilla brewing-stand path does (see {@code PotionUtilsMixin}).
     * The base roll passed in is expected to already be clamped to the natural essence range.
     */
    private static EffectProvider applyBrewBonusesFromConfigs(EssenceHolder holder, Essence essence, int baseAmplifier, int baseDuration, List<ModifyBrewedPotionPower.Configuration> configs) {
        MobEffect effect = essence.effect().value();
        if (effect == null || configs.isEmpty()) {
            return new DirectEffectProvider(holder, baseAmplifier, baseDuration);
        }
        MobEffectCategory category = effect.getCategory();
        int ampMod = 0;
        float durMult = 1.0f;
        for (ModifyBrewedPotionPower.Configuration cfg : configs) {
            if (!ModifyBrewedPotionPower.appliesTo(cfg, category)) continue;
            ampMod += cfg.amplifierModifier();
            durMult *= cfg.durationMultiplier();
        }
        int finalAmp = Math.max(0, baseAmplifier + ampMod);
        int finalDur = Math.max(1, (int) Math.ceil(baseDuration * durMult));
        return new DirectEffectProvider(holder, finalAmp, finalDur);
    }

    /**
     * Grants {@code amount} mastery XP to the player's {@link AlchemyProfile}, if Ars has one.
     * Silent (no level-up event/sounds) — the Alchemist's brew is constant background gain.
     */
    public static boolean grantMasteryXp(Player player, int amount) {
        if (amount <= 0) return false;
        AlchemyProfile profile = Alchemy.get(player.level()).profileOf(player);
        if (profile == null) return false;
        return profile.mastery().addXpNoEvent(amount);
    }

    /**
     * Reveals up to {@code count} previously-unknown ingredients on the player's
     * {@link AlchemyProfile} as bases. No-op if there are no undiscovered ingredients.
     */
    public static int discoverRandomIngredients(Player player, int count, RandomSource random) {
        if (count <= 0) return 0;
        AlchemyProfile profile = Alchemy.get(player.level()).profileOf(player);
        if (profile == null) return 0;
        KnownIngredients known = profile.knownIngredients();
        List<net.minecraft.world.item.Item> undiscovered = new ArrayList<>();
        Alchemy.get(player.level()).ingredients().forEach((item, props) -> {
            if (!known.getEntries().containsKey(item)) undiscovered.add(item);
        });
        if (undiscovered.isEmpty()) return 0;
        Collections.shuffle(undiscovered, new java.util.Random(random.nextLong()));
        int reveal = Math.min(count, undiscovered.size());
        for (int i = 0; i < reveal; i++) known.discoverAsBase(undiscovered.get(i));
        return reveal;
    }
}
