package dev.muon.raven_dnd_origins.mixin.compat.raven_core;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.seniors.justlevelingfork.common.capability.AptitudeCapability;
import dev.muon.raven_core.leveling.LevelingUtils;
import dev.muon.raven_dnd_origins.power.InnateAptitudeBonusPower;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = LevelingUtils.class, remap = false)
public class LevelingUtilsMixin {

    @ModifyExpressionValue(
            method = "getPlayerLevel(Lnet/minecraft/world/entity/player/Player;)I",
            at = @At(value = "INVOKE", target = "Lcom/seniors/justlevelingfork/common/capability/AptitudeCapability;getGlobalLevel()I")
    )
    private static int subtractInnateFromGlobalLevel(int original, Player player) {
        return Math.max(original - raven_dnd_origins$innateBonusTotal(player), 0);
    }

    @ModifyExpressionValue(
            method = "getPlayerLevelProgress(Lnet/minecraft/world/entity/player/Player;)D",
            at = @At(value = "INVOKE", target = "Lcom/seniors/justlevelingfork/common/capability/AptitudeCapability;getGlobalLevel()I")
    )
    private static int subtractInnateFromGlobalProgress(int original, Player player) {
        return Math.max(original - raven_dnd_origins$innateBonusTotal(player), 0);
    }

    @Unique
    private static int raven_dnd_origins$innateBonusTotal(Player player) {
        AptitudeCapability cap = AptitudeCapability.get(player);
        if (cap == null) return 0;
        return InnateAptitudeBonusPower.sumBonusesForAptitudes(player, cap.aptitudeLevel.keySet());
    }
}
