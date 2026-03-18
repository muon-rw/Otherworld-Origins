package dev.muon.otherworldorigins.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.muon.otherworldorigins.power.ModPowers;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * When the player has prevent_repair_penalty power, anvil repairs do not increase
 * the item's repair cost (enchantment cost penalty).
 *
 * Need priority=900 to apply before Apotheosis, since they @Redirect here
 */
@Mixin(value = AnvilMenu.class, priority = 900)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {

    // Note: this is ignored by mixin
    public AnvilMenuMixin(@Nullable MenuType<?> type, int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(type, containerId, playerInventory, access);
    }

    @WrapOperation(
            method = "createResult",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/AnvilMenu;calculateIncreasedRepairCost(I)I"
            )
    )
    private int otherworldorigins$preventRepairPenalty(int oldRepairCost, Operation<Integer> original) {
        IPowerContainer powerContainer = ApoliAPI.getPowerContainer(this.player);
        if (powerContainer != null && powerContainer.hasPower(ModPowers.PREVENT_REPAIR_PENALTY.get())) {
            return oldRepairCost;
        }
        return original.call(oldRepairCost);
    }
}
