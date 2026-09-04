package dev.muon.raven_dnd_origins.mixin;

import dev.muon.raven_dnd_origins.util.shapeshift.ShapeshiftEquipmentHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.EnchantmentAttributeEffect;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Location-triggered enchantment attributes (Soul Speed) are applied straight onto the attribute
 * map on every block change, bypassing the item modifier scan that wild shape suppresses. Skip
 * them for armor while a suppressing form is active; deactivation still runs so nothing leaks.
 */
@Mixin(EnchantmentAttributeEffect.class)
public abstract class EnchantmentAttributeEffectMixin {

    @Inject(method = "onChangedBlock", at = @At("HEAD"), cancellable = true)
    private void raven_dnd_origins$skipSuppressedArmorAttributes(ServerLevel level, int enchantmentLevel, EnchantedItemInUse item,
                                                                 Entity entity, Vec3 pos, boolean applyTransientEffects,
                                                                 CallbackInfo ci) {
        if (entity instanceof Player player && item.inSlot() != null && item.inSlot().isArmor()
                && ShapeshiftEquipmentHandler.suppressesArmorModifiers(player)) {
            ci.cancel();
        }
    }
}
