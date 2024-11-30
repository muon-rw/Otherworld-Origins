package dev.muon.otherworldorigins.mixin.compat.apotheosis;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.muon.otherworldorigins.restrictions.EnchantmentRestrictions;
import dev.shadowsoffire.apotheosis.ench.Ench;
import dev.shadowsoffire.apotheosis.ench.enchantments.masterwork.EndlessQuiverEnchant;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = EndlessQuiverEnchant.class, remap = false)
public class EndlessQuiverEnchantMixin {
    @ModifyReturnValue(method = "isTrulyInfinite", at = @At("RETURN"))
    private boolean otherworld$restrictEndlessQuiver(boolean original, @Local(argsOnly = true) Player player) {
        if (!EnchantmentRestrictions.isEnchantmentAllowed(player, Ench.Enchantments.ENDLESS_QUIVER.get())) {
            return false;
        }
        return original;
    }
}
