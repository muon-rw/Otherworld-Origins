package dev.muon.otherworldorigins.mixin.compat.bettercombat;

import dev.muon.otherworldorigins.power.ShapeshiftPower;
import dev.muon.otherworldorigins.util.ShapeshiftWeaponAttributes;
import net.bettercombat.api.AttackHand;
import net.bettercombat.logic.PlayerAttackHelper;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * When a shapeshifted player attacks bare-handed, provides Better Combat
 * with custom WeaponAttributes so the attack uses cone/sweep/AoE hit
 * detection instead of a vanilla single-target punch.
 */
@Mixin(value = PlayerAttackHelper.class, remap = false)
public class PlayerAttackHelperMixin {

    @Inject(method = "getCurrentAttack", at = @At("HEAD"), cancellable = true)
    private static void otherworldorigins$shapeshiftAttack(Player player, int comboCount,
                                                           CallbackInfoReturnable<AttackHand> cir) {
        ShapeshiftPower.Configuration config = ShapeshiftPower.getActiveShapeshiftConfig(player);
        if (config == null || config.allowTools()) return;

        AttackHand hand = ShapeshiftWeaponAttributes.resolve(config, comboCount);
        if (hand != null) {
            cir.setReturnValue(hand);
        }
    }
}
