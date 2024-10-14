package dev.muon.otherworldorigins.mixin.compat.legendarysurvivaloverhaul;

import com.llamalad7.mixinextras.sugar.Local;
import dev.muon.otherworldorigins.power.ModPowers;
import dev.muon.otherworldorigins.power.ModifyThirstExhaustionPower;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
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
        IPowerContainer powerContainer = ApoliAPI.getPowerContainer(player);
        if (powerContainer != null) {
            var playerPowers = powerContainer.getPowers(ModPowers.MODIFY_THIRST_EXHAUSTION.get());
            float totalModifier = playerPowers.stream()
                    .map(holder -> holder.value().getConfiguration())
                    .map(ModifyThirstExhaustionPower.Configuration::amount)
                    .reduce(0f, Float::sum);
            if (totalModifier != 0) {
                return exhaustion * totalModifier;
            }
        }

        return exhaustion;
    }


}
