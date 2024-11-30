package dev.muon.otherworldorigins.mixin.ench_restrictions;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.muon.otherworldorigins.restrictions.EnchantmentRestrictions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {

    @ModifyReturnValue(method = "getEnchantmentLevel(Lnet/minecraft/world/item/enchantment/Enchantment;Lnet/minecraft/world/entity/LivingEntity;)I", at = @At("RETURN"), require = 1)
    private static int otherworldorigins$restrictEnchantments(int original, Enchantment enchantment, LivingEntity entity) {
        if (!(entity instanceof Player player)) return original;
        if (original > 0 && !EnchantmentRestrictions.isEnchantmentAllowed(player, enchantment)) {
            return 0;
        }
        return original;
    }

}