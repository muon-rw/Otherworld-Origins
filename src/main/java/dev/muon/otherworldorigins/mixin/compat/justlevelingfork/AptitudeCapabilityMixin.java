package dev.muon.otherworldorigins.mixin.compat.justlevelingfork;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.seniors.justlevelingfork.common.capability.AptitudeCapability;
import com.seniors.justlevelingfork.registry.aptitude.Aptitude;
import dev.muon.otherworldorigins.power.InnateAptitudeBonusPower;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = AptitudeCapability.class, remap = false)
public class AptitudeCapabilityMixin {


    @ModifyExpressionValue(
            method = "addAptitudeLevel",
            at = @At(value = "FIELD", target = "Lcom/seniors/justlevelingfork/handler/HandlerCommonConfig;aptitudeMaxLevel:I")
    )
    private int modifyAptitudeMaxLevel(int originalMaxLevel, Aptitude aptitude, int addLvl) {
        AptitudeCapability self = (AptitudeCapability) (Object) this;
        Player player = otherworld$getPlayerFromCapability(self);

        if (player != null) {
            int bonus = InnateAptitudeBonusPower.getBonus(player, aptitude.getName());
            return originalMaxLevel + bonus;
        }

        return originalMaxLevel;
    }
    @Unique
    private Player otherworld$getPlayerFromCapability(AptitudeCapability capability) {
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            for (ServerLevel level : ServerLifecycleHooks.getCurrentServer().getAllLevels()) {
                for (Player player : level.players()) {
                    LazyOptional<AptitudeCapability> cap = player.getCapability(com.seniors.justlevelingfork.registry.RegistryCapabilities.APTITUDE);
                    if (cap.isPresent() && cap.resolve().get() == capability) {
                        return player;
                    }
                }
            }
        }
        return null;
    }
}