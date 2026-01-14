package dev.muon.otherworldorigins.mixin.ench_restrictions;

import com.llamalad7.mixinextras.sugar.Local;
import dev.muon.otherworldorigins.restrictions.EnchantmentRestrictions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BowItem.class)
public class BowItemMixin {

    @Inject(
            method = "releaseUsing",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z")
    )
    private void otherworldorigins$adjustArrowDamage(ItemStack pStack, Level pLevel, LivingEntity pEntityLiving, int pTimeLeft, CallbackInfo ci,
                                                     @Local Player player,
                                                     @Local AbstractArrow abstractarrow, @Local(ordinal = 2) int j) {
        if (!EnchantmentRestrictions.isEnchantmentAllowed(player, Enchantments.POWER_ARROWS)) {
            double damageReduction = j * 0.5D + 0.5D;
            abstractarrow.setBaseDamage(abstractarrow.getBaseDamage() - damageReduction);
        }

//        if (!EnchantmentRestrictions.isEnchantmentAllowed(player, Enchantments.PUNCH_ARROWS)) {
//            abstractarrow.setKnockback(0);
//        }

//        if (!EnchantmentRestrictions.isEnchantmentAllowed(player, Enchantments.FLAMING_ARROWS)) {
//            abstractarrow.clearFire();
//        }
    }
}