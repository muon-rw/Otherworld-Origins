package dev.muon.otherworldorigins.mixin.compat.apotheosis;

import dev.muon.otherworldorigins.power.ModPowers;
import dev.muon.otherworldorigins.power.ModifyEnchantmentCostPower;
import dev.shadowsoffire.placebo.util.EnchantmentUtils;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EnchantmentUtils.class, remap = false)
public class EnchantmentUtilsMixin {

    @Inject(method = "Ldev/shadowsoffire/placebo/util/EnchantmentUtils;chargeExperience(Lnet/minecraft/world/entity/player/Player;I)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private static void modifyEnchantmentCost(Player player, int cost, CallbackInfoReturnable<Boolean> cir) {
        IPowerContainer powerContainer = ApoliAPI.getPowerContainer(player);
        if (powerContainer != null) {
            var playerPowers = powerContainer.getPowers(ModPowers.MODIFY_ENCHANTMENT_COST.get());
            if (!playerPowers.isEmpty()) {
                float totalModifier = playerPowers.stream()
                        .map(holder -> holder.value().getConfiguration())
                        .map(ModifyEnchantmentCostPower.Configuration::amount)
                        .reduce(1f, (a, b) -> a * (1 - b));

                int modifiedCost = Math.max(1, Math.round(cost * totalModifier));

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

}