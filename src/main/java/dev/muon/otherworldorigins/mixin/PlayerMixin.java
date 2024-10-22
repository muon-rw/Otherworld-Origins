package dev.muon.otherworldorigins.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.muon.otherworldorigins.util.EnchantmentRestrictions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public class PlayerMixin {

    @ModifyExpressionValue(
            method = "attack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getDamageBonus(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/MobType;)F"),
            require = 1
    )
    private float otherworldorigins$restrictDamageEnchantments(float original) {
        Player self = (Player) (Object) this;
        if (!EnchantmentRestrictions.isEnchantmentAllowed(self, Enchantments.SHARPNESS) &&
                !EnchantmentRestrictions.isEnchantmentAllowed(self, Enchantments.SMITE) &&
                !EnchantmentRestrictions.isEnchantmentAllowed(self, Enchantments.BANE_OF_ARTHROPODS)) {
            return 0f;
        }
        return original;
    }
}
