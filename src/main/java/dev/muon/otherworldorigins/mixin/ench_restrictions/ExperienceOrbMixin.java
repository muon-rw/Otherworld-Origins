package dev.muon.otherworldorigins.mixin.ench_restrictions;

import dev.muon.otherworldorigins.restrictions.EnchantmentRestrictions;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ExperienceOrb.class)
public class ExperienceOrbMixin {

    @Inject(method = "repairPlayerItems", at = @At("HEAD"), cancellable = true)
    private void otherworldorigins$restrictMendingEnchantment(Player pPlayer, int pRepairAmount, CallbackInfoReturnable<Integer> cir) {
        if (!EnchantmentRestrictions.isEnchantmentAllowed(pPlayer, Enchantments.MENDING)) {
            cir.setReturnValue(pRepairAmount);
        }
    }
}