package dev.muon.raven_dnd_origins.mixin.ench_restrictions;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.muon.raven_dnd_origins.restrictions.EnchantmentRestrictions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ArrowItem.class)
public class ArrowItemMixin {

    @ModifyReturnValue(method = "isInfinite", at = @At("RETURN"))
    private boolean raven_dnd_origins$restrictInfinityEnchantment(boolean original, @Local(argsOnly = true) LivingEntity shooter) {
        if (original && shooter instanceof Player player && !EnchantmentRestrictions.isEnchantmentAllowed(player, Enchantments.INFINITY)) {
            return false;
        }
        return original;
    }
}
