package dev.muon.otherworldorigins.mixin.compat.legendarysurvivaloverhaul;

import com.llamalad7.mixinextras.sugar.Local;
import dev.muon.otherworldorigins.power.ModifyThirstExhaustionPower;
import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.apoli.util.modifier.ModifierUtil;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import sfiomn.legendarysurvivaloverhaul.util.internal.ThirstUtilInternal;

@Mixin(value = ThirstUtilInternal.class, remap = false)
public class ThirstUtilInternalMixin {

    @ModifyVariable(method = "addExhaustion",
            at = @At(value = "INVOKE", target = "Lsfiomn/legendarysurvivaloverhaul/common/capabilities/thirst/ThirstCapability;addThirstExhaustion(F)V"),
            argsOnly = true)
    private float modifyExhaustion(float exhaustion, @Local(argsOnly = true) Player player) {
        double totalModifier = PowerHolderComponent.getPowers(player, ModifyThirstExhaustionPower.class).stream()
                .filter(ModifyThirstExhaustionPower::isActive)
                .mapToDouble(powerType -> ModifierUtil.applyModifiers(player, powerType.getModifiers(), 1.0))
                .reduce(1.0, (a, b) -> a * b);
        
        if (totalModifier != 1.0) {
            return (float) (exhaustion * totalModifier);
        }

        return exhaustion;
    }
}
