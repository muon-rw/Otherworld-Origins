package dev.muon.raven_dnd_origins.mixin.ench_restrictions;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.muon.raven_dnd_origins.restrictions.EnchantmentRestrictions;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperMixin {

    /**
     * Every equipment-slot enchantment effect (thorns, feather falling, post-attack effects, tick effects)
     * is dispatched through this visitor, so skipping it here is the general restriction hook.
     */
    @WrapWithCondition(
            method = "runIterationOnItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentInSlotVisitor;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentInSlotVisitor;accept(Lnet/minecraft/core/Holder;ILnet/minecraft/world/item/enchantment/EnchantedItemInUse;)V"
            ),
            require = 1
    )
    private static boolean raven_dnd_origins$skipRestrictedSlotEffects(
            EnchantmentHelper.EnchantmentInSlotVisitor visitor, Holder<Enchantment> enchantment, int level, EnchantedItemInUse item,
            @Local(argsOnly = true) LivingEntity entity
    ) {
        return !(entity instanceof Player player) || EnchantmentRestrictions.isEnchantmentAllowed(player, item.itemStack(), enchantment);
    }

    /**
     * Item-only effects (damage bonuses, attribute modifiers) have no entity here; the restriction applies when a caller
     * published the item's user through {@link EnchantmentRestrictions#asItemUser}.
     */
    @WrapWithCondition(
            method = "runIterationOnItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentVisitor;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentVisitor;accept(Lnet/minecraft/core/Holder;I)V"
            ),
            require = 1
    )
    private static boolean raven_dnd_origins$skipRestrictedItemEffects(
            EnchantmentHelper.EnchantmentVisitor visitor, Holder<Enchantment> enchantment, int level,
            @Local(argsOnly = true) ItemStack stack
    ) {
        return EnchantmentRestrictions.isAllowedForItemUser(stack, enchantment);
    }

    @WrapMethod(method = {
            "modifyDamage(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;F)F",
            "modifyFallBasedDamage(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;F)F",
            "modifyArmorEffectiveness(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;F)F",
            "modifyKnockback(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;F)F"
    })
    private static float raven_dnd_origins$restrictByAttacker(ServerLevel level, ItemStack tool, Entity target, DamageSource source, float value,
                                                             Operation<Float> original) {
        return EnchantmentRestrictions.asItemUser(source.getEntity(), () -> original.call(level, tool, target, source, value));
    }
}
