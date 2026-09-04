package dev.muon.raven_dnd_origins.mixin.ench_restrictions;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.muon.raven_dnd_origins.restrictions.EnchantmentRestrictions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Infinity is an ammo-use enchantment effect in 1.21, so restricting it means forcing normal ammo
 * consumption rather than blocking a boolean.
 */
@Mixin(ProjectileWeaponItem.class)
public class ProjectileWeaponItemMixin {

    @WrapOperation(
            method = "useAmmo",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;processAmmoUse(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;I)I"
            )
    )
    private static int raven_dnd_origins$restrictInfinityAmmoUse(
            ServerLevel level, ItemStack weapon, ItemStack ammo, int count, Operation<Integer> original,
            @Local(argsOnly = true) LivingEntity shooter
    ) {
        int used = original.call(level, weapon, ammo, count);
        if (used < count && shooter instanceof Player player
                && !EnchantmentRestrictions.isEnchantmentAllowed(player, Enchantments.INFINITY)) {
            return count;
        }
        return used;
    }
}
