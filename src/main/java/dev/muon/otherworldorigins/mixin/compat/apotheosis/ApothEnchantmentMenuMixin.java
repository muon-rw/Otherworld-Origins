package dev.muon.otherworldorigins.mixin.compat.apotheosis;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.muon.otherworldorigins.power.ModPowers;
import dev.muon.otherworldorigins.power.ModifyEnchantmentCostPower;
import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Reduces the enchantment level cost for players with the ModifyEnchantmentCost power.
 * This affects both the displayed level requirement and the enchantment selection.
 */
@Mixin(value = ApothEnchantmentMenu.class, remap = false)
public class ApothEnchantmentMenuMixin {

    @Final
    @Shadow
    protected Player player;

    /**
     * Modifies the enchantment cost after it's calculated.
     * // TODO:
     * This reduces both the level requirement and affects enchantment quality proportionally.
     * Needs to affect only requirement
     */
    @ModifyExpressionValue(
            method = "lambda$slotsChanged$1",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/shadowsoffire/apotheosis/ench/table/RealEnchantmentHelper;getEnchantmentCost(Lnet/minecraft/util/RandomSource;IFLnet/minecraft/world/item/ItemStack;)I"
            )
    )
    private int otherworldorigins$modifyEnchantmentCost(int originalCost) {
        if (this.player == null) return originalCost;
        
        IPowerContainer powerContainer = ApoliAPI.getPowerContainer(this.player);
        if (powerContainer == null) return originalCost;
        
        var playerPowers = powerContainer.getPowers(ModPowers.MODIFY_ENCHANTMENT_COST.get());
        if (playerPowers.isEmpty()) return originalCost;
        
        // Calculate total modifier (multiplicative reduction)
        float totalModifier = playerPowers.stream()
                .map(holder -> holder.value().getConfiguration())
                .map(ModifyEnchantmentCostPower.Configuration::amount)
                .reduce(1f, (a, b) -> a * (1 - b));
        
        // Apply modifier and ensure minimum cost of 1
        return Math.max(1, Math.round(originalCost * totalModifier));
    }
}
