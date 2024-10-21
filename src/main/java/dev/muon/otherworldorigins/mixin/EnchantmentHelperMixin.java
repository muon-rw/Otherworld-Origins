package dev.muon.otherworldorigins.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.muon.otherworldorigins.util.EnchantmentRestrictions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

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


    @WrapOperation(
            method = "doPostHurtEffects",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;runIterationOnInventory(Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentVisitor;Ljava/lang/Iterable;)V"),
            require = 1
    )
    private static void otherworldorigins$restrictThorns(@Coerce Object /*EnchantmentVisitor*/ visitor, Iterable<ItemStack> items, Operation<Void> original, LivingEntity target, Entity attacker) {
        if (target instanceof Player player) {
            if (EnchantmentRestrictions.isEnchantmentAllowed(player, Enchantments.THORNS)) {
                original.call(visitor, items);
            }
            return;
        } else {
            original.call(visitor, items);
        }
    }
}