package dev.muon.raven_dnd_origins.mixin.compat.justlevelingfork;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.seniors.justlevelingfork.common.capability.AptitudeCapability;
import com.seniors.justlevelingfork.registry.aptitude.Aptitude;
import dev.muon.raven_dnd_origins.power.InnateAptitudeBonusPower;
import dev.muon.raven_dnd_origins.selection.SelectionSessions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AptitudeCapability.class, remap = false)
public class AptitudeCapabilityBonusMixin {

    @Inject(method = "addAptitudeLevel", at = @At("HEAD"))
    private void captureOwner(Aptitude aptitude, int addLvl, CallbackInfo ci,
                              @Share("owner") LocalRef<Player> owner) {
        owner.set(raven_dnd_origins$ownerOf((AptitudeCapability) (Object) this));
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

    // JustLevelingFork holds the capability on the player itself and exposes no back-reference,
    // so the owner has to be found by scanning. Only runs on aptitude level-up.
    @Unique
    private Player raven_dnd_origins$ownerOf(AptitudeCapability capability) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return null;
        }
        for (ServerLevel level : server.getAllLevels()) {
            for (Player player : level.players()) {
                if (AptitudeCapability.get(player) == capability) {
                    return player;
                }
            }
        }
        return null;
    }
}
