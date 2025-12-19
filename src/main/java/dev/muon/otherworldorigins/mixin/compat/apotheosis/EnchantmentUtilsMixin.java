package dev.muon.otherworldorigins.mixin.compat.apotheosis;

import dev.muon.otherworldorigins.power.ModifyEnchantmentCostPower;
import dev.shadowsoffire.placebo.util.EnchantmentUtils;
import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.apoli.util.modifier.ModifierUtil;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EnchantmentUtils.class, remap = false)
public class EnchantmentUtilsMixin {

    @Inject(method = "Ldev/shadowsoffire/placebo/util/EnchantmentUtils;chargeExperience(Lnet/minecraft/world/entity/player/Player;I)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private static void modifyEnchantmentCost(Player player, int cost, CallbackInfoReturnable<Boolean> cir) {
        double totalModifier = PowerHolderComponent.getPowers(player, ModifyEnchantmentCostPower.class).stream()
                .filter(ModifyEnchantmentCostPower::isActive)
                .mapToDouble(powerType -> ModifierUtil.applyModifiers(player, powerType.getModifiers(), 1.0))
                .reduce(1.0, (a, b) -> a * (1 - b));

        if (totalModifier != 1.0) {
            int modifiedCost = Math.max(1, (int) Math.round(cost * totalModifier));

            int playerExperience = EnchantmentUtils.getExperience(player);

            if (playerExperience >= modifiedCost) {
                player.giveExperiencePoints(-modifiedCost);

                if (EnchantmentUtils.getExperience(player) <= 0) player.experienceProgress = 0F;
                cir.setReturnValue(true);
            } else {
                cir.setReturnValue(false);
            }
        }
    }
}
