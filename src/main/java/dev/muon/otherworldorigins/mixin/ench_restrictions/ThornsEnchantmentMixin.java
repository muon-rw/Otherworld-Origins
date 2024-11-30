package dev.muon.otherworldorigins.mixin.ench_restrictions;

import dev.muon.otherworldorigins.restrictions.EnchantmentRestrictions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ThornsEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThornsEnchantment.class)
public class ThornsEnchantmentMixin {

    @Inject(method = "doPostHurt", at = @At("HEAD"), cancellable = true)
    private void otherworldorigins$restrictThornsDamage(LivingEntity pUser, Entity pAttacker, int pLevel, CallbackInfo ci) {
        if (pUser instanceof Player player && !EnchantmentRestrictions.isEnchantmentAllowed(player, Enchantments.THORNS)) {
            ci.cancel();
        }
    }
}