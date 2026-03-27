package dev.muon.otherworldorigins.mixin.compat.apotheosis;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.muon.otherworldorigins.power.ModPowers;
import dev.muon.otherworldorigins.power.ModifyEnchantmentCostPower;
import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Reduces the enchantment level cost for players with the ModifyEnchantmentCost power.
 * The cost reduction only affects the level requirement and XP charge — enchantment
 * quality is preserved by restoring the original power level in getEnchantmentList.
 */
@Pseudo
@Mixin(value = ApothEnchantmentMenu.class, remap = false)
public class ApothEnchantmentMenuMixin {

    @Final
    @Shadow
    protected Player player;

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
