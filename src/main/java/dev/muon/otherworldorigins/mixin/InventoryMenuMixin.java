package dev.muon.otherworldorigins.mixin;

import dev.muon.otherworldorigins.power.ShapeshiftPower;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin {

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void otherworldorigins$preventShiftClickToArmor(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (index < InventoryMenu.INV_SLOT_START) return;

        ShapeshiftPower.Configuration config = ShapeshiftPower.getActiveShapeshiftConfig(player);
        if (config == null || !config.preventEquipment()) return;

        InventoryMenu self = (InventoryMenu) (Object) this;
        Slot slot = self.getSlot(index);
        if (slot.hasItem()) {
            EquipmentSlot equipSlot = Mob.getEquipmentSlotForItem(slot.getItem());
            if (equipSlot.isArmor()) {
                cir.setReturnValue(ItemStack.EMPTY);
            }
        }
    }
}
