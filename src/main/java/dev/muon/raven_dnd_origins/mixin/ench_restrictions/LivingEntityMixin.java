package dev.muon.raven_dnd_origins.mixin.ench_restrictions;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.muon.raven_dnd_origins.restrictions.EnchantmentRestrictions;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.BiConsumer;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    // Only the add pass is gated, so modifiers granted before a class change still come off on unequip
    @WrapOperation(
            method = "collectEquipmentChanges",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V",
                    ordinal = 1
            ),
            require = 1
    )
    private void raven_dnd_origins$restrictAddedEnchantmentModifiers(ItemStack stack, EquipmentSlot slot,
                                                                     BiConsumer<Holder<Attribute>, AttributeModifier> action,
                                                                     Operation<Void> original) {
        EnchantmentRestrictions.asItemUser((LivingEntity) (Object) this, () -> original.call(stack, slot, action));
    }
}
