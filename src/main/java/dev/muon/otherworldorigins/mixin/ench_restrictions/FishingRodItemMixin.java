package dev.muon.otherworldorigins.mixin.ench_restrictions;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.muon.otherworldorigins.restrictions.EnchantmentRestrictions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FishingRodItem.class)
public class FishingRodItemMixin {

    @ModifyExpressionValue(
            method = "use",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getFishingLuckBonus(Lnet/minecraft/world/item/ItemStack;)I"),
            require = 1
    )
    private int otherworldorigins$restrictLuckOfTheSea(int original, @Local Player player) {
        if (!EnchantmentRestrictions.isEnchantmentAllowed(player, Enchantments.FISHING_LUCK)) {
            return 0;
        }
        return original;
    }
}