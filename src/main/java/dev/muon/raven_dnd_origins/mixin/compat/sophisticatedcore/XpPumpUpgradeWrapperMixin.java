package dev.muon.raven_dnd_origins.mixin.compat.sophisticatedcore;

import dev.muon.raven_dnd_origins.restrictions.EnchantmentRestrictions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantments;
import net.p3pp3rf1y.sophisticatedcore.upgrades.xppump.XpPumpUpgradeWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = XpPumpUpgradeWrapper.class, remap = false)
public class XpPumpUpgradeWrapperMixin {

    @Inject(method = "mendItems", at = @At("HEAD"), cancellable = true)
    private void raven_dnd_origins$restrictMendingEnchantment(Player player, CallbackInfo ci) {
        if (!EnchantmentRestrictions.isEnchantmentAllowed(player, Enchantments.MENDING)) {
            ci.cancel();
        }
    }
}
