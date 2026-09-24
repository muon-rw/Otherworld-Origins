package dev.muon.raven_dnd_origins.mixin.compat.irons_spellbooks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.muon.raven_dnd_origins.item.ModUpgradeOrbTypes;
import dev.muon.raven_dnd_origins.util.RangedWeapons;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import io.redspace.ironsspellbooks.gui.arcane_anvil.ArcaneAnvilMenu;
import io.redspace.ironsspellbooks.registries.ComponentRegistry;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ArcaneAnvilMenu.class, remap = false)
public class ArcaneAnvilMenuMixin {

    @WrapOperation(
            method = "createResult",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/api/util/Utils;canBeUpgraded(Lnet/minecraft/world/item/ItemStack;)Z"
            )
    )
    private boolean raven_dnd_origins$allowArcheryOrbOnRangedWeapons(ItemStack baseItemStack, Operation<Boolean> original,
                                                                     @Local(ordinal = 2) ItemStack modifierItemStack) {
        return original.call(baseItemStack)
                || !ServerConfigs.UPGRADE_BLACKLIST_ITEMS.contains(baseItemStack.getItem())
                && ModUpgradeOrbTypes.ARROW_DAMAGE.equals(modifierItemStack.get(ComponentRegistry.UPGRADE_ORB_TYPE))
                && RangedWeapons.hasRangedWeaponProperties(baseItemStack);
    }
}
