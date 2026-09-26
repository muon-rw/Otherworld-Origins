package dev.muon.raven_dnd_origins.mixin.compat.bettercombat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.muon.raven_dnd_origins.power.ShapeshiftPower;
import dev.muon.raven_dnd_origins.util.shapeshift.ShapeshiftWeaponAttributes;
import net.bettercombat.BetterCombatMod;
import net.bettercombat.api.AttackHand;
import net.bettercombat.logic.EntityAttributeHelper;
import net.bettercombat.logic.PlayerAttackHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.muon.raven_dnd_origins.util.shapeshift.ShapeshiftToolHandler;

/**
 * When a shapeshifted player attacks bare-handed, provides Better Combat
 * with custom WeaponAttributes so the attack uses cone/sweep/AoE hit
 * detection instead of a vanilla single-target punch.
 */
@Mixin(value = PlayerAttackHelper.class, remap = false)
public class PlayerAttackHelperMixin {

    @Inject(method = "getCurrentAttack", at = @At("HEAD"), cancellable = true)
    private static void raven_dnd_origins$shapeshiftAttack(Player player, int comboCount,
                                                           CallbackInfoReturnable<AttackHand> cir) {
        ShapeshiftPower.Configuration config = ShapeshiftPower.getActiveShapeshiftConfig(player);
        if (config == null || config.allowTools()) return;

        if (ShapeshiftToolHandler.isWeaponOrTool(player.getMainHandItem())) {

            cir.setReturnValue(null);

            return;

        }

        AttackHand hand = ShapeshiftWeaponAttributes.resolve(player, config, comboCount);
        if (hand != null) {
            cir.setReturnValue(hand);
        }
    }

    @ModifyReturnValue(method = "isDualWielding(Lnet/minecraft/world/entity/player/Player;)Z", at = @At("RETURN"))
    private static boolean raven_dnd_origins$preventShapeshiftDualWield(boolean original, Player player) {
        if (!original) return false;
        ShapeshiftPower.Configuration config = ShapeshiftPower.getActiveShapeshiftConfig(player);
        return config == null || config.allowTools();
    }

    // Feeds Player#entityInteractionRange, the server's hit gate, which must match the client's scaled form reach
    @ModifyReturnValue(method = "getRangeWithWeapon(Lnet/minecraft/world/entity/player/Player;D)D", at = @At("RETURN"))
    private static double raven_dnd_origins$shapeshiftReach(double original, Player player, double interactionRangeValue) {
        ShapeshiftPower.Configuration config = ShapeshiftPower.getActiveShapeshiftConfig(player);
        if (config == null || config.allowTools()) return original;
        ItemStack held = player.getMainHandItem();
        if (ShapeshiftToolHandler.isWeaponOrTool(held) || EntityAttributeHelper.itemHasRangeAttribute(held)) return original;
        double formReach = PlayerAttackHelper.combineAttackRange(ShapeshiftWeaponAttributes.getOrBuild(config), interactionRangeValue);
        return formReach * Math.max(1.0F, BetterCombatMod.config.getAttackRangeMultiplierForScale(player.getScale()));
    }
}
