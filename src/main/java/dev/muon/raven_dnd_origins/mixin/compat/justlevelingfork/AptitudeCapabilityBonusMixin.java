package dev.muon.raven_dnd_origins.mixin.compat.justlevelingfork;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.seniors.justlevelingfork.common.capability.AptitudeCapability;
import com.seniors.justlevelingfork.registry.aptitude.Aptitude;
import dev.muon.raven_core.leveling.AptitudeCapabilityOwner;
import dev.muon.raven_dnd_origins.power.InnateAptitudeBonusPower;
import dev.muon.raven_dnd_origins.selection.SelectionSessions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AptitudeCapability.class, remap = false)
public class AptitudeCapabilityBonusMixin {

    @Inject(method = "addAptitudeLevel", at = @At("HEAD"))
    private void captureOwner(Aptitude aptitude, int addLvl, CallbackInfo ci,
                              @Share("owner") LocalRef<Player> owner) {
        owner.set(((AptitudeCapabilityOwner) (Object) this).raven_core$owner());
    }

    @ModifyExpressionValue(
            method = "addAptitudeLevel",
            at = @At(value = "FIELD", target = "Lcom/seniors/justlevelingfork/handler/HandlerCommonConfig;aptitudeMaxLevel:I", opcode = Opcodes.GETFIELD)
    )
    private int raiseMaxLevelByInnateBonus(int maxLevel, Aptitude aptitude, int addLvl,
                                           @Share("owner") LocalRef<Player> owner) {
        Player player = owner.get();
        return player == null ? maxLevel : maxLevel + InnateAptitudeBonusPower.getBonus(player, aptitude.getName());
    }

    @Inject(method = "addAptitudeLevel", at = @At("RETURN"))
    private void promptLevelGatedSelection(Aptitude aptitude, int addLvl, CallbackInfo ci,
                                           @Share("owner") LocalRef<Player> owner) {
        if (owner.get() instanceof ServerPlayer serverPlayer) {
            SelectionSessions.promptLevelGated(serverPlayer);
        }
    }

    // Runs on save load and on every client sync; without it an aptitude raised past the cap by an
    // innate bonus is cut back to the cap and the bonus is silently lost.
    @WrapOperation(
            method = "deserializeNBT(Lnet/minecraft/nbt/CompoundTag;)V",
            at = @At(value = "INVOKE", target = "Lcom/seniors/justlevelingfork/common/capability/AptitudeCapability;clampAptitudeLevel(I)I")
    )
    private int raiseClampByInnateBonus(int level, Operation<Integer> original, @Local Aptitude aptitude) {
        int clamped = original.call(level);
        Player owner = ((AptitudeCapabilityOwner) (Object) this).raven_core$owner();
        if (owner == null || level <= clamped) return clamped;
        return Math.min(level, clamped + InnateAptitudeBonusPower.getBonus(owner, aptitude.getName()));
    }
}
