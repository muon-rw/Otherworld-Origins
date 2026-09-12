package dev.muon.raven_dnd_origins.mixin.ench_restrictions;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.muon.raven_dnd_origins.restrictions.EnchantmentRestrictions;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;
import java.util.function.Predicate;

@Mixin(ExperienceOrb.class)
public class ExperienceOrbMixin {

    /**
     * Vanilla picks a random damaged Mending item across the player's slots; items whose Mending the player
     * is not allowed to use are filtered out of that pick so the orb still repairs the allowed ones.
     */
    @WrapOperation(
            method = "repairPlayerItems",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getRandomItemWith(Lnet/minecraft/core/component/DataComponentType;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Predicate;)Ljava/util/Optional;"
            )
    )
    private Optional<EnchantedItemInUse> raven_dnd_origins$skipRestrictedMendingItems(
            DataComponentType<?> componentType, LivingEntity entity, Predicate<ItemStack> filter, Operation<Optional<EnchantedItemInUse>> original
    ) {
        if (!(entity instanceof ServerPlayer player)) {
            return original.call(componentType, entity, filter);
        }
        Predicate<ItemStack> allowed = stack -> filter.test(stack)
                && EnchantmentRestrictions.isEnchantmentAllowed(player, stack, Enchantments.MENDING);
        return original.call(componentType, entity, allowed);
    }
}
