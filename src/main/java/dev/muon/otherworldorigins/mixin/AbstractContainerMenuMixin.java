package dev.muon.otherworldorigins.mixin;

import dev.muon.otherworldorigins.capability.BrewerTrackerCapability;
import dev.muon.otherworldorigins.power.ShapeshiftPower;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {

    @Shadow public abstract Slot getSlot(int slotId);
    @Shadow public abstract boolean isValidSlotIndex(int slotIndex);

    @Unique
    private static final int otherworld$BREWING_INGREDIENT_SLOT = 3;

    @Unique
    private boolean otherworld$brewingIngredientWasEmpty;

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void otherworldorigins$preventArmorSlotClick(int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
        if (slotId < 0 || !isValidSlotIndex(slotId)) return;

        ShapeshiftPower.Configuration config = ShapeshiftPower.getActiveShapeshiftConfig(player);
        if (config == null || !config.preventEquipment()) return;

        Slot slot = getSlot(slotId);
        if (slot.container == player.getInventory()
                && slot.getSlotIndex() >= 36
                && slot.getSlotIndex() <= 39) {
            ci.cancel();
        }
    }

    @Inject(method = "clicked", at = @At("HEAD"))
    private void otherworld$captureBrewingIngredientPre(int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
        if ((Object) this instanceof BrewingStandMenu menu) {
            this.otherworld$brewingIngredientWasEmpty =
                    ((BrewingStandMenuAccessor) menu).otherworld$brewingStand()
                            .getItem(otherworld$BREWING_INGREDIENT_SLOT).isEmpty();
        }
    }

    @Inject(method = "clicked", at = @At("TAIL"))
    private void otherworld$captureBrewingIngredientPost(int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
        if ((Object) this instanceof BrewingStandMenu menu) {
            var brewingStand = ((BrewingStandMenuAccessor) menu).otherworld$brewingStand();
            if (this.otherworld$brewingIngredientWasEmpty
                    && !brewingStand.getItem(otherworld$BREWING_INGREDIENT_SLOT).isEmpty()
                    && brewingStand instanceof BrewingStandBlockEntity be) {
                BrewerTrackerCapability.setBrewer(be, player.getUUID());
            }
        }
    }
}
