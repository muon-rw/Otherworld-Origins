package dev.muon.otherworldorigins.mixin.compat.apotheosis;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.muon.otherworldorigins.power.EnchantingKnowledgePower;
import dev.muon.otherworldorigins.power.ModPowers;
import dev.muon.otherworldorigins.power.ModifyEnchantmentCostPower;
import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu;
import dev.shadowsoffire.apotheosis.ench.table.ClueMessage;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.ArrayList;
import java.util.List;

/**
 * Apotheosis enchanting table hooks: {@link ModifyEnchantmentCostPower} cost display/quality,
 * and {@link EnchantingKnowledgePower} full clue lists (via {@code new ClueMessage} args).
 * Injection points use MixinExtras {@link Expression} targets instead of ordinal-based bytecode.
 */
@Pseudo
@Mixin(value = ApothEnchantmentMenu.class, remap = false)
public class ApothEnchantmentMenuMixin {

    @Final
    @Shadow
    protected Player player;

    /**
     * Snapshot of the last {@link ApothEnchantmentMenu#getEnchantmentList} result in
     * {@code lambda$slotsChanged$1}, before Apotheosis removes entries for the primary clue and packet.
     */
    @Unique
    private List<EnchantmentInstance> otherworldorigins$fullCluePool;

    @Definition(
            id = "getEnchantmentList",
            method = "Ldev/shadowsoffire/apotheosis/ench/table/ApothEnchantmentMenu;getEnchantmentList(Lnet/minecraft/world/item/ItemStack;II)Ljava/util/List;"
    )
    @Expression("this.getEnchantmentList(?, ?, ?)")
    @WrapOperation(
            method = "lambda$slotsChanged$1",
            at = @At(value = "MIXINEXTRAS:EXPRESSION", remap = false)
    )
    private List<EnchantmentInstance> otherworldorigins$captureFullCluePool(
            ApothEnchantmentMenu instance, ItemStack stack, int enchantSlot, int level, Operation<List<EnchantmentInstance>> original) {

        // Not @ModifyReturnValue: the invoke result is stored to a local, not returned from the lambda.
        List<EnchantmentInstance> result = original.call(this, stack, enchantSlot, level);
        if (EnchantingKnowledgePower.has(this.player) && result != null && !result.isEmpty()) {
            this.otherworldorigins$fullCluePool = new ArrayList<>(result);
        } else {
            this.otherworldorigins$fullCluePool = null;
        }
        return result;
    }

    @Definition(id = "ClueMessage", type = ClueMessage.class)
    @Expression("new ClueMessage(?, ?, ?)")
    @ModifyArgs(
            method = "lambda$slotsChanged$1",
            at = @At(value = "MIXINEXTRAS:EXPRESSION", remap = false)
    )
    private void otherworldorigins$sendAllClues(Args args) {
        if (this.otherworldorigins$fullCluePool != null) {
            args.set(1, new ArrayList<>(this.otherworldorigins$fullCluePool));
            args.set(2, true);
            this.otherworldorigins$fullCluePool = null;
        }
    }

    @Unique
    private float otherworldorigins$getCostModifier() {
        if (this.player == null) return 1f;

        IPowerContainer powerContainer = ApoliAPI.getPowerContainer(this.player);
        if (powerContainer == null) return 1f;

        var playerPowers = powerContainer.getPowers(ModPowers.MODIFY_ENCHANTMENT_COST.get());
        if (playerPowers.isEmpty()) return 1f;

        return playerPowers.stream()
                .map(holder -> holder.value().getConfiguration())
                .map(ModifyEnchantmentCostPower.Configuration::amount)
                .reduce(1f, (a, b) -> a * (1 - b));
    }

    /**
     * Reduces the enchantment cost stored in {@code costs[]}, which controls both
     * the displayed level requirement and the XP charged when enchanting.
     */
    @ModifyExpressionValue(
            method = "lambda$slotsChanged$1",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/shadowsoffire/apotheosis/ench/table/RealEnchantmentHelper;getEnchantmentCost(Lnet/minecraft/util/RandomSource;IFLnet/minecraft/world/item/ItemStack;)I"
            )
    )
    private int otherworldorigins$modifyEnchantmentCost(int originalCost) {
        float modifier = otherworldorigins$getCostModifier();
        if (modifier >= 1f) return originalCost;
        return Math.max(1, Math.round(originalCost * modifier));
    }

    /**
     * Reverses the cost reduction when selecting enchantments, so enchantment quality
     * is based on the original power level rather than the reduced display cost.
     */
    @ModifyArg(
            method = {"lambda$slotsChanged$1", "lambda$clickMenuButton$0"},
            // These lambda targets
            remap = true,
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/shadowsoffire/apotheosis/ench/table/ApothEnchantmentMenu;getEnchantmentList(Lnet/minecraft/world/item/ItemStack;II)Ljava/util/List;"
            ),
            index = 2
    )
    private int otherworldorigins$restoreOriginalLevelForQuality(int reducedLevel) {
        float modifier = otherworldorigins$getCostModifier();
        if (modifier >= 1f || modifier <= 0f) return reducedLevel;
        return Math.round(reducedLevel / modifier);
    }
}
