package dev.muon.otherworldorigins.mixin.compat.justlevelingfork;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.seniors.justlevelingfork.common.capability.AptitudeCapability;
import com.seniors.justlevelingfork.registry.RegistryCapabilities;
import com.seniors.justlevelingfork.registry.aptitude.Aptitude;
import dev.muon.otherworldorigins.power.InnateAptitudeBonusPower;
import dev.muon.otherworldorigins.util.FeatHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(value = AptitudeCapability.class, remap = false)
public class AptitudeCapabilityMixin {

    @Unique private Player otherworld$player;

    @Inject(method = "addAptitudeLevel", at = @At("HEAD"))
    private void onAddAptitudeLevelStart(Aptitude aptitude, int addLvl, CallbackInfo ci) {
        AptitudeCapability self = (AptitudeCapability) (Object) this;
        otherworld$player = otherworld$getPlayerFromCapability(self);
    }

    @ModifyExpressionValue(
            method = "addAptitudeLevel",
            at = @At(value = "FIELD", target = "Lcom/seniors/justlevelingfork/handler/HandlerCommonConfig;aptitudeMaxLevel:I")
    )
    private int increaseMaxByOriginBonus(int originalMaxLevel, Aptitude aptitude, int addLvl) {
        if (otherworld$player != null) {
            int bonus = InnateAptitudeBonusPower.getBonus(otherworld$player, aptitude.getName());
            return originalMaxLevel + bonus;
        }
        return originalMaxLevel;
    }

    @Inject(method = "addAptitudeLevel", at = @At("RETURN"))
    private void onAddAptitudeLevelEnd(Aptitude aptitude, int addLvl, CallbackInfo ci) {
        if (otherworld$player instanceof ServerPlayer serverPlayer) {
            FeatHandler.checkForFeats(serverPlayer);
        }
    }

    @Unique
    private Player otherworld$getPlayerFromCapability(AptitudeCapability capability) {
        if (ServerLifecycleHooks.getCurrentServer() != null) {
            for (ServerLevel level : ServerLifecycleHooks.getCurrentServer().getAllLevels()) {
                for (Player player : level.players()) {
                    LazyOptional<AptitudeCapability> cap = player.getCapability(RegistryCapabilities.APTITUDE);
                    if (cap.isPresent() && cap.resolve().get() == capability) {
                        return player;
                    }
                }
            }
        }
        return null;
    }
}