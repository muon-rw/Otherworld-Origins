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
import org.spongepowered.asm.mixin.Unique;
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

    /* Better Combat asks for attributes on every equipment read of every living entity, so the
       power walk behind the shapeshift lookup is cached for the client tick it was made in. */
    @Unique
    private static Player raven_dnd_origins$cachedPlayer;
    @Unique
    private static int raven_dnd_origins$cachedTick = -1;
    @Unique
    private static ShapeshiftPower.Configuration raven_dnd_origins$cachedConfig;

    @ModifyReturnValue(
            method = "getAttributes(Lnet/minecraft/world/item/ItemStack;)Lnet/bettercombat/api/WeaponAttributes;",
            at = @At("RETURN"),
            require = 1
    )
    private static WeaponAttributes raven_dnd_origins$shapeshiftAttributes(WeaponAttributes original, ItemStack itemStack) {
        // The integrated server shares this class and must resolve items as a dedicated server would
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) return original;
        Player player = minecraft.player;
        if (player == null || itemStack == null) return original;

        var config = raven_dnd_origins$activeConfigThisTick(player);
        if (config == null || config.allowTools()) return original;

        // A blocked weapon must not become the form's natural attack; with no attributes Better Combat

        // leaves the swing to vanilla, where the client key handler refuses it.

        if (ShapeshiftToolHandler.isWeaponOrTool(itemStack)) return null;

        return ShapeshiftWeaponAttributes.getOrBuild(config);
    }

    @Unique
    private static ShapeshiftPower.Configuration raven_dnd_origins$activeConfigThisTick(Player player) {
        if (player != raven_dnd_origins$cachedPlayer || player.tickCount != raven_dnd_origins$cachedTick) {
            raven_dnd_origins$cachedPlayer = player;
            raven_dnd_origins$cachedTick = player.tickCount;
            raven_dnd_origins$cachedConfig = ShapeshiftPower.getActiveShapeshiftConfig(player);
        }
        return raven_dnd_origins$cachedConfig;
    }
}
