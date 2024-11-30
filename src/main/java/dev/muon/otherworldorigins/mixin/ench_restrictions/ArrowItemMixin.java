package dev.muon.otherworldorigins.mixin.ench_restrictions;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.muon.otherworldorigins.restrictions.EnchantmentRestrictions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ArrowItem.class, remap = false)
public class ArrowItemMixin {

    @ModifyReturnValue(method = "isInfinite", at = @At("RETURN"))
    private boolean otherworldorigins$restrictInfinityEnchantment(boolean original, @Local(argsOnly = true) Player player) {
        if (original && !EnchantmentRestrictions.isEnchantmentAllowed(player, Enchantments.INFINITY_ARROWS)) {
            return false;
        }
        return original;
    }
}