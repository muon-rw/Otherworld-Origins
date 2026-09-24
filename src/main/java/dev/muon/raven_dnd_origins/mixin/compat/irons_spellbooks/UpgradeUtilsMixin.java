package dev.muon.raven_dnd_origins.mixin.compat.irons_spellbooks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.muon.raven_dnd_origins.item.ModUpgradeOrbTypes;
import io.redspace.ironsspellbooks.item.armor.UpgradeOrbType;
import io.redspace.ironsspellbooks.util.UpgradeUtils;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.function.BiConsumer;

@Mixin(value = UpgradeUtils.class, remap = false)
public class UpgradeUtilsMixin {

    @WrapOperation(
            method = "handleAttributeEvent",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/util/UpgradeUtils;collectAndRemovePreexistingAttribute(Ljava/util/List;Lnet/minecraft/core/Holder;Lnet/minecraft/world/entity/ai/attributes/AttributeModifier$Operation;Ljava/util/function/BiConsumer;)D"
            )
    )
    private static double raven_dnd_origins$keepArcheryOrbSeparate(List<ItemAttributeModifiers.Entry> modifiers,
                                                                    Holder<Attribute> attribute,
                                                                    AttributeModifier.Operation operation,
                                                                    BiConsumer<Holder<Attribute>, AttributeModifier> removeCallback,
                                                                    Operation<Double> original,
                                                                    @Local Holder<UpgradeOrbType> orb) {
        return orb.is(ModUpgradeOrbTypes.ARROW_DAMAGE) ? 0.0 : original.call(modifiers, attribute, operation, removeCallback);
    }
}
