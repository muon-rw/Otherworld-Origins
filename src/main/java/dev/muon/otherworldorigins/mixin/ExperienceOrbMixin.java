package dev.muon.otherworldorigins.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.muon.otherworldorigins.util.EnchantmentRestrictions;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ExperienceOrb.class)
public class ExperienceOrbMixin {

    @WrapMethod(method = "repairPlayerItems", require = 1)
    private int otherworldorigins$restrictMendingEnchantment(Player pPlayer, int pRepairAmount, Operation<Integer> original) {
        if (!EnchantmentRestrictions.isEnchantmentAllowed(pPlayer, Enchantments.MENDING)) {
            return 0;
        }
        return original.call(pPlayer, pRepairAmount);
    }
}