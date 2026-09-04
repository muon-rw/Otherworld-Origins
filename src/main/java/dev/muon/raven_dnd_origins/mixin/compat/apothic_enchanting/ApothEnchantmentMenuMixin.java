package dev.muon.raven_dnd_origins.mixin.compat.apothic_enchanting;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.muon.raven_dnd_origins.power.EnchantingKnowledgePower;
import dev.muon.raven_dnd_origins.power.IncreaseEnchantingLevelsPower;
import dev.muon.raven_dnd_origins.power.ModPowers;
import dev.muon.raven_dnd_origins.power.ModifyEnchantmentCostPower;
import dev.overgrown.apoli.power.PowerLookup;
import dev.shadowsoffire.apothic_enchanting.table.ApothEnchantmentMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.ArrayList;
import java.util.List;

/**
 * Apothic Enchanting table hooks: {@link ModifyEnchantmentCostPower} cost display/quality,
 * {@link IncreaseEnchantingLevelsPower} rolled enchantment levels,
 * and {@link EnchantingKnowledgePower} full clue lists (via the outgoing CluePayload args).
 */
@Mixin(value = ApothEnchantmentMenu.class, remap = false)
public class ApothEnchantmentMenuMixin {

    @Final
    @Shadow
    protected Player player;

    /**
     * Snapshot of the last {@code getEnchantmentList} result in {@code lambda$slotsChanged$1},
     * before Apothic Enchanting removes entries for the primary clue and the payload.
     */
    @Unique
    private List<EnchantmentInstance> raven_dnd_origins$fullCluePool;

    @WrapOperation(
            method = "lambda$slotsChanged$1",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/shadowsoffire/apothic_enchanting/table/ApothEnchantmentMenu;getEnchantmentList(Lnet/minecraft/world/item/ItemStack;II)Ljava/util/List;"
            )
    )
    private List<EnchantmentInstance> raven_dnd_origins$captureFullCluePool(
            ApothEnchantmentMenu instance, ItemStack stack, int enchantSlot, int level, Operation<List<EnchantmentInstance>> original) {

        List<EnchantmentInstance> result = original.call(instance, stack, enchantSlot, raven_dnd_origins$preReductionLevel(level));
        if (EnchantingKnowledgePower.has(this.player) && result != null && !result.isEmpty()) {
            this.raven_dnd_origins$fullCluePool = new ArrayList<>(result);
        } else {
            this.raven_dnd_origins$fullCluePool = null;
        }
        return result;
    }

    @ModifyArgs(
            method = "lambda$slotsChanged$1",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/shadowsoffire/apothic_enchanting/payloads/CluePayload;<init>(ILjava/util/List;Z)V"
            )
    )
    private void raven_dnd_origins$sendAllClues(Args args) {
        if (this.raven_dnd_origins$fullCluePool != null) {
            args.set(1, new ArrayList<>(this.raven_dnd_origins$fullCluePool));
            args.set(2, true);
            this.raven_dnd_origins$fullCluePool = null;
        }
    }

    /**
     * Applied after the candidate list for a slot is built (including the infusion override);
     * matches client clue formatting in {@link ApothEnchantmentScreenMixin}.
     */
    @ModifyReturnValue(
            method = "getEnchantmentList(Lnet/minecraft/world/item/ItemStack;II)Ljava/util/List;",
            at = @At("RETURN")
    )
    private List<EnchantmentInstance> raven_dnd_origins$boostEnchantmentLevels(List<EnchantmentInstance> original) {
        return IncreaseEnchantingLevelsPower.applyBonus(this.player, original);
    }

    @Unique
    private float raven_dnd_origins$getCostModifier() {
        if (this.player == null) return 1f;
        float[] modifier = {1f};
        PowerLookup.forEach(this.player, ModPowers.MODIFY_ENCHANTMENT_COST, ModifyEnchantmentCostPower.Configuration.class,
                cfg -> modifier[0] *= (1 - cfg.amount()));
        return modifier[0];
    }

    /**
     * Reduces the enchantment cost stored in {@code costs[]}, which controls both
     * the displayed level requirement and the XP charged when enchanting.
     */
    @ModifyExpressionValue(
            method = "lambda$slotsChanged$1",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/shadowsoffire/apothic_enchanting/table/ApothEnchantmentHelper;getEnchantmentCost(Lnet/minecraft/util/RandomSource;IFLnet/minecraft/world/item/ItemStack;)I"
            )
    )
    private int raven_dnd_origins$modifyEnchantmentCost(int originalCost) {
        float modifier = raven_dnd_origins$getCostModifier();
        if (modifier >= 1f) return originalCost;
        return Math.max(1, Math.round(originalCost * modifier));
    }

    /**
     * Reverses the cost reduction when selecting enchantments, so enchantment quality
     * is based on the original power level rather than the reduced display cost.
     * The {@code slotsChanged} call site is covered by
     * {@link #raven_dnd_origins$captureFullCluePool} instead, which already wraps that invoke.
     */
    @ModifyArg(
            method = "lambda$clickMenuButton$0",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/shadowsoffire/apothic_enchanting/table/ApothEnchantmentMenu;getEnchantmentList(Lnet/minecraft/world/item/ItemStack;II)Ljava/util/List;"
            ),
            index = 2
    )
    private int raven_dnd_origins$restoreOriginalLevelForQuality(int reducedLevel) {
        return raven_dnd_origins$preReductionLevel(reducedLevel);
    }

    @Unique
    private int raven_dnd_origins$preReductionLevel(int reducedLevel) {
        float modifier = raven_dnd_origins$getCostModifier();
        if (modifier >= 1f || modifier <= 0f) return reducedLevel;
        return Math.round(reducedLevel / modifier);
    }
}
