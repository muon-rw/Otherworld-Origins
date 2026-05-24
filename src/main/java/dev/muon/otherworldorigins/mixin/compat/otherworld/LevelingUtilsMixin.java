package dev.muon.otherworldorigins.mixin.compat.otherworld;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.seniors.justlevelingfork.common.capability.AptitudeCapability;
import dev.muon.otherworld.leveling.LevelingUtils;
import dev.muon.otherworldorigins.power.InnateAptitudeBonusPower;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = LevelingUtils.class, remap = false)
public class LevelingUtilsMixin {

    @ModifyExpressionValue(
            method = "Ldev/muon/otherworld/leveling/LevelingUtils;getPlayerLevel(Lnet/minecraft/world/entity/player/Player;)I",
            at = @At(value = "INVOKE", target = "Lcom/seniors/justlevelingfork/common/capability/AptitudeCapability;getGlobalLevel()I"),
            require = 1
    )
    private static int subtractInnateFromGlobalLevel(int original, Player player) {
        return Math.max(original - otherworld$innateBonusTotal(player), 0);
    }

    @ModifyExpressionValue(
            method = "Ldev/muon/otherworld/leveling/LevelingUtils;getPlayerLevelProgress(Lnet/minecraft/world/entity/player/Player;)D",
            at = @At(value = "INVOKE", target = "Lcom/seniors/justlevelingfork/common/capability/AptitudeCapability;getGlobalLevel()I"),
            require = 1
    )
    private static int subtractInnateFromGlobalProgress(int original, Player player) {
        return Math.max(original - otherworld$innateBonusTotal(player), 0);
    }

    @Unique
    private static int otherworld$innateBonusTotal(Player player) {
        AptitudeCapability cap = AptitudeCapability.get(player);
        if (cap == null) return 0;
        return InnateAptitudeBonusPower.sumBonusesForAptitudes(player, cap.aptitudeLevel.keySet());
    }
}
