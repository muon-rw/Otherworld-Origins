package dev.muon.raven_dnd_origins.mixin.compat.sophisticatedcore;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.muon.raven_dnd_origins.restrictions.EnchantmentRestrictions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.p3pp3rf1y.sophisticatedcore.upgrades.xppump.XpPumpUpgradeWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(value = XpPumpUpgradeWrapper.class, remap = false)
public class XpPumpUpgradeWrapperMixin {

    @WrapOperation(
            method = "mendItems",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/p3pp3rf1y/sophisticatedcore/upgrades/xppump/XpPumpUpgradeWrapper;getRandomDamagedItemWithMending(Lnet/minecraft/world/entity/player/Player;)Ljava/util/Optional;"
            )
    )
    private Optional<ItemStack> raven_dnd_origins$skipRestrictedMendingItems(
            XpPumpUpgradeWrapper wrapper, Player player, Operation<Optional<ItemStack>> original
    ) {
        return original.call(wrapper, player)
                .filter(stack -> EnchantmentRestrictions.isEnchantmentAllowed(player, stack, Enchantments.MENDING));
    }
}
