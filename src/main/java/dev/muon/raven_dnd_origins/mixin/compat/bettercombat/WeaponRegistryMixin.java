package dev.muon.raven_dnd_origins.mixin.compat.bettercombat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.muon.raven_dnd_origins.power.ShapeshiftPower;
import dev.muon.raven_dnd_origins.util.shapeshift.ShapeshiftWeaponAttributes;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import dev.muon.raven_dnd_origins.util.shapeshift.ShapeshiftToolHandler;

/**
 * When the local player is shapeshifted (!allowTools), always returns the
 * shapeshift weapon attributes regardless of what item is held. This ensures
 * BC's entire attack pipeline (upswing, target finding, sweep/cone hit
 * detection) activates instead of vanilla single-target punching.
 */
@Mixin(value = WeaponRegistry.class, remap = false)
public class WeaponRegistryMixin {

    @ModifyReturnValue(
            method = "getAttributes(Lnet/minecraft/world/item/ItemStack;)Lnet/bettercombat/api/WeaponAttributes;",
            at = @At("RETURN"),
            require = 1
    )
    private static WeaponAttributes raven_dnd_origins$shapeshiftAttributes(WeaponAttributes original, ItemStack itemStack) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return original;

        var config = ShapeshiftPower.getActiveShapeshiftConfig(player);
        if (config == null || config.allowTools()) return original;

        // A blocked weapon must not become the form's natural attack; with no attributes Better Combat

        // leaves the swing to vanilla, where the client key handler refuses it.

        if (ShapeshiftToolHandler.isWeaponOrTool(itemStack)) return null;

        return ShapeshiftWeaponAttributes.getOrBuild(config);
    }
}
