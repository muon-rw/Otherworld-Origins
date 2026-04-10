package dev.muon.otherworldorigins.mixin.compat.bettercombat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.muon.otherworldorigins.power.ShapeshiftPower;
import dev.muon.otherworldorigins.util.shapeshift.ShapeshiftWeaponAttributes;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

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
    private static WeaponAttributes otherworldorigins$shapeshiftAttributes(WeaponAttributes original, ItemStack itemStack) {
        if (!FMLEnvironment.dist.isClient()) return original;

        Player player = Minecraft.getInstance().player;
        if (player == null) return original;

        ShapeshiftPower.Configuration config = ShapeshiftPower.getActiveShapeshiftConfig(player);
        if (config == null || config.allowTools()) return original;

        return ShapeshiftWeaponAttributes.getOrBuild(config);
    }
}
