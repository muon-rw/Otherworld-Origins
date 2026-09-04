package dev.muon.raven_dnd_origins.mixin;

import dev.muon.raven_dnd_origins.capability.BrewerTrackerCapability;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {

    @Unique
    private static final int raven_dnd_origins$BREWING_INGREDIENT_SLOT = 3;

    @Unique
    private boolean raven_dnd_origins$brewingIngredientWasEmpty;

    @Inject(method = "clicked", at = @At("HEAD"))
    private void raven_dnd_origins$captureBrewingIngredientPre(int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
        if ((Object) this instanceof BrewingStandMenu menu) {
            this.raven_dnd_origins$brewingIngredientWasEmpty =
                    ((BrewingStandMenuAccessor) menu).raven_dnd_origins$brewingStand()
                            .getItem(raven_dnd_origins$BREWING_INGREDIENT_SLOT).isEmpty();
        }
    }

    @Inject(method = "clicked", at = @At("TAIL"))
    private void raven_dnd_origins$captureBrewingIngredientPost(int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
        if ((Object) this instanceof BrewingStandMenu menu) {
            Container brewingStand = ((BrewingStandMenuAccessor) menu).raven_dnd_origins$brewingStand();
            if (this.raven_dnd_origins$brewingIngredientWasEmpty
                    && !brewingStand.getItem(raven_dnd_origins$BREWING_INGREDIENT_SLOT).isEmpty()
                    && brewingStand instanceof BrewingStandBlockEntity be) {
                BrewerTrackerCapability.setBrewer(be, player.getUUID());
            }
        }
    }
}
