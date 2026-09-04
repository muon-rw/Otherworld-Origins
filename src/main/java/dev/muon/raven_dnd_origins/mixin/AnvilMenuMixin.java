package dev.muon.raven_dnd_origins.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.muon.raven_dnd_origins.power.ModPowers;
import dev.muon.raven_dnd_origins.util.EnhancedRepairLogic;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Anvil repairs do not raise the item's repair cost for holders of prevent_repair_penalty.
 * priority=900 so this applies before Apotheosis, which redirects the same call.
 */
@Mixin(value = AnvilMenu.class, priority = 900)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {

    // Ignored by mixin
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
    private int raven_dnd_origins$preventRepairPenalty(int oldRepairCost, Operation<Integer> original) {
        if (PowerLookup.hasActive(this.player, ModPowers.PREVENT_REPAIR_PENALTY)) {
            return oldRepairCost;
        }
        return original.call(oldRepairCost);
    }

    @Inject(method = "createResult", at = @At("TAIL"))
    private void raven_dnd_origins$enhancedRepair(CallbackInfo ci) {
        ItemStack left = this.inputSlots.getItem(0);
        ItemStack result = this.resultSlots.getItem(0);
        EnhancedRepairLogic.onAnvilRepair(this.player, left, result);
    }
}
