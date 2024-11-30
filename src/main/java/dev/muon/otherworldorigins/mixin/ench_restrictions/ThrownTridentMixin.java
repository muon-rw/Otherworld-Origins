package dev.muon.otherworldorigins.mixin.ench_restrictions;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.muon.otherworldorigins.restrictions.EnchantmentRestrictions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ThrownTrident.class)
public class ThrownTridentMixin {

    @WrapOperation(
            method = {"<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)V", "readAdditionalSaveData"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getLoyalty(Lnet/minecraft/world/item/ItemStack;)I")
    )
    private int otherworldorigins$restrictLoyalty(ItemStack stack, Operation<Integer> original) {
        ThrownTrident trident = (ThrownTrident) (Object) this;

        if (trident.getOwner() instanceof Player player) {
            if (!EnchantmentRestrictions.isEnchantmentAllowed(player, Enchantments.LOYALTY)) {
                return 0;
            }
        }
        return original.call(stack);
    }
}