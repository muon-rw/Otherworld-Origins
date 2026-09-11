package dev.muon.raven_dnd_origins.mixin.compat.justlevelingfork;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.seniors.justlevelingfork.common.capability.AptitudeCapability;
import com.seniors.justlevelingfork.network.packet.common.AptitudeLevelUpSP;
import com.seniors.justlevelingfork.registry.aptitude.Aptitude;
import dev.muon.raven_dnd_origins.power.InnateAptitudeBonusPower;
import net.minecraft.server.level.ServerPlayer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = AptitudeLevelUpSP.class, remap = false)
public class AptitudeLevelUpSPMixin {

    @ModifyExpressionValue(
            method = "handle(Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At(value = "FIELD", target = "Lcom/seniors/justlevelingfork/handler/HandlerCommonConfig;aptitudeMaxLevel:I", opcode = Opcodes.GETFIELD)
    )
    private int raiseMaxLevelByInnateBonus(int maxLevel, ServerPlayer sender, @Local Aptitude aptitude) {
        return maxLevel + InnateAptitudeBonusPower.getBonus(sender, aptitude.getName());
    }

    @ModifyExpressionValue(
            method = "handle(Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At(value = "FIELD", target = "Lcom/seniors/justlevelingfork/handler/HandlerCommonConfig;playersMaxGlobalLevel:I", opcode = Opcodes.GETFIELD)
    )
    private int raiseGlobalMaxLevelByInnateBonus(int maxLevel, ServerPlayer sender, @Local AptitudeCapability capability) {
        return maxLevel + InnateAptitudeBonusPower.sumBonusesForAptitudes(sender, capability.aptitudeLevel.keySet());
    }
}
