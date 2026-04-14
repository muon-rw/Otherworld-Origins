package dev.muon.otherworldorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.shadowsoffire.apotheosis.ench.Ench;
import dev.shadowsoffire.apotheosis.ench.asm.EnchHooks;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds flat levels to each enchantment rolled at the Apotheosis enchanting table (clues and result).
 * When {@link Configuration#bypass_max_level} is false, results are capped to
 * {@link EnchHooks#getMaxLevel(Enchantment)} (Apotheosis config max). When true, the full bonus is
 * always applied with no max-level cap. Apotheosis infusion is left unchanged.
 */
public class IncreaseEnchantingLevelsPower extends PowerFactory<IncreaseEnchantingLevelsPower.Configuration> {
    public IncreaseEnchantingLevelsPower() {
        super(Configuration.CODEC);
    }

    public record Configuration(int amount, boolean bypass_max_level) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("amount").forGetter(Configuration::amount),
                Codec.BOOL.optionalFieldOf("bypass_max_level", false).forGetter(Configuration::bypass_max_level)
        ).apply(instance, Configuration::new));

        @Override
        public boolean isConfigurationValid() {
            return amount >= 0;
        }
    }

    public static int getBonus(Player player) {
        if (player == null) {
            return 0;
        }
        IPowerContainer powerContainer = ApoliAPI.getPowerContainer(player);
        if (powerContainer == null) {
            return 0;
        }
        return powerContainer.getPowers(ModPowers.INCREASE_ENCHANTING_LEVELS.get()).stream()
                .map(holder -> holder.value().getConfiguration().amount())
                .reduce(0, Integer::sum);
    }

    /**
     * True if any active {@link Configuration} sets {@link Configuration#bypass_max_level}
     * (no cap on applied bonus).
     */
    public static boolean bypassesMaxLevel(Player player) {
        if (player == null) {
            return false;
        }
        IPowerContainer powerContainer = ApoliAPI.getPowerContainer(player);
        if (powerContainer == null) {
            return false;
        }
        return powerContainer.getPowers(ModPowers.INCREASE_ENCHANTING_LEVELS.get()).stream()
                .anyMatch(holder -> holder.value().getConfiguration().bypass_max_level());
    }

    /**
     * Tooltip line for a clue: starts from {@link Enchantment#getFullname(int)} (preserves curse /
     * mod colors and any overridden formatting), then appends the {@code [base + from power]} suffix.
     */
    public static Component formatClueLine(Enchantment enchantment, int finalLevel, int powerBonus) {
        if (powerBonus <= 0) {
            return enchantment.getFullname(finalLevel);
        }
        int baseLevel = Math.max(1, finalLevel - powerBonus);
        int fromPower = finalLevel - baseLevel;
        if (fromPower <= 0) {
            return enchantment.getFullname(finalLevel);
        }
        MutableComponent line = enchantment.getFullname(finalLevel).copy();
        line.append(Component.translatable(
                "tooltip.otherworldorigins.enchant_level_breakdown",
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
            int cap = bypass ? Integer.MAX_VALUE : EnchHooks.getMaxLevel(inst.enchantment);
            int newLevel = Math.min(inst.level + bonus, cap);
            if (newLevel == inst.level) {
                out.add(inst);
            } else {
                out.add(new EnchantmentInstance(inst.enchantment, newLevel));
            }
        }
        return out;
    }

    private static boolean isApotheosisInfusion(Enchantment enchantment) {
        return enchantment == Ench.Enchantments.INFUSION.get();
    }
}
