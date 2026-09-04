package dev.muon.raven_dnd_origins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import dev.shadowsoffire.apothic_enchanting.Ench;
import dev.shadowsoffire.apothic_enchanting.asm.EnchHooks;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds flat levels to each enchantment rolled at the Apotheosis enchanting table (clues and result).
 * When {@link Configuration#bypassMaxLevel} is false, results are capped to
 * {@link EnchHooks#getMaxLevel(Enchantment)} (Apotheosis config max). When true, the full bonus is
 * always applied with no max-level cap. Apotheosis infusion is left unchanged.
 */
public final class IncreaseEnchantingLevelsPower extends PowerType<IncreaseEnchantingLevelsPower.Configuration> {

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("amount").forGetter(Configuration::amount),
            Codec.BOOL.optionalFieldOf("bypass_max_level", false).forGetter(Configuration::bypassMaxLevel)
    ).apply(i, Configuration::new));

    @Override
    public MapCodec<Configuration> configCodec() {
        return CODEC;
    }

    public static int getBonus(Player player) {
        int[] bonus = new int[1];
        PowerLookup.forEach(player, ModPowers.INCREASE_ENCHANTING_LEVELS, Configuration.class, cfg -> bonus[0] += cfg.amount());
        return bonus[0];
    }

    /**
     * True if any active {@link Configuration} sets {@link Configuration#bypassMaxLevel}
     * (no cap on applied bonus).
     */
    public static boolean bypassesMaxLevel(Player player) {
        boolean[] bypass = new boolean[]{false};
        PowerLookup.forEach(player, ModPowers.INCREASE_ENCHANTING_LEVELS, Configuration.class, cfg -> {
            if (cfg.bypassMaxLevel()) bypass[0] = true;
        });
        return bypass[0];
    }

    /**
     * Tooltip line for a clue: starts from {@link Enchantment#getFullname(Holder, int)} (preserves
     * curse / mod colors and any overridden formatting), then appends the {@code [base + from power]}
     * suffix.
     */
    public static Component formatClueLine(Holder<Enchantment> enchantment, int finalLevel, int powerBonus) {
        if (powerBonus <= 0) {
            return Enchantment.getFullname(enchantment, finalLevel);
        }
        int baseLevel = Math.max(1, finalLevel - powerBonus);
        int fromPower = finalLevel - baseLevel;
        if (fromPower <= 0) {
            return Enchantment.getFullname(enchantment, finalLevel);
        }
        MutableComponent line = Enchantment.getFullname(enchantment, finalLevel).copy();
        line.append(Component.translatable(
                "tooltip.raven_dnd_origins.enchant_level_breakdown",
                Component.translatable("enchantment.level." + baseLevel),
                Component.translatable("enchantment.level." + fromPower)
        ));
        return line;
    }

    public static List<EnchantmentInstance> applyBonus(Player player, List<EnchantmentInstance> list) {
        if (list == null || list.isEmpty()) {
            return list;
        }
        int bonus = getBonus(player);
        if (bonus <= 0) {
            return list;
        }
        boolean bypass = bypassesMaxLevel(player);
        List<EnchantmentInstance> out = new ArrayList<>(list.size());
        for (EnchantmentInstance inst : list) {
            if (isApotheosisInfusion(inst.enchantment)) {
                out.add(inst);
                continue;
            }
            int cap = bypass ? Integer.MAX_VALUE : EnchHooks.getMaxLevel(inst.enchantment.value());
            int newLevel = Math.min(inst.level + bonus, cap);
            if (newLevel == inst.level) {
                out.add(inst);
            } else {
                out.add(new EnchantmentInstance(inst.enchantment, newLevel));
            }
        }
        return out;
    }

    private static boolean isApotheosisInfusion(Holder<Enchantment> enchantment) {
        return enchantment.is(Ench.Enchantments.INFUSION);
    }

    public record Configuration(int amount, boolean bypassMaxLevel) {
    }
}
