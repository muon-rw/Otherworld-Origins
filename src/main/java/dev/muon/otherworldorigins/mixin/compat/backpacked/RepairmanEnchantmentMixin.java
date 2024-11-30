package dev.muon.otherworldorigins.mixin.compat.backpacked;

import com.mrcrayfish.backpacked.enchantment.RepairmanEnchantment;
import dev.muon.otherworldorigins.restrictions.EnchantmentRestrictions;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RepairmanEnchantment.class,remap = false)
public class RepairmanEnchantmentMixin {

    @Inject(method = "onPickupExperience", at = @At("HEAD"), cancellable = true)
    private static void otherworldorigins$restrictRepairmanEnchantment(Player player, ExperienceOrb experienceOrb, CallbackInfoReturnable<Boolean> cir) {
        if (!EnchantmentRestrictions.isEnchantmentAllowed(player, Enchantments.MENDING)) {
            cir.setReturnValue(false);
        }
    }
}