package dev.muon.otherworldorigins.mixin.compat.medieval;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.seniors.justlevelingfork.common.capability.AptitudeCapability;
import dev.muon.medieval.leveling.LevelingUtils;
import dev.muon.otherworldorigins.power.InnateAptitudeBonusPower;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.LazyOptional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = LevelingUtils.class, remap = false)
public class LevelingUtilsMixin {

    @ModifyExpressionValue(
            method = "Ldev/muon/medieval/leveling/LevelingUtils;getPlayerLevel(Lnet/minecraft/world/entity/player/Player;)I",
            at = @At(value = "INVOKE", target = "Lcom/seniors/justlevelingfork/common/capability/AptitudeCapability;getGlobalLevel()I"),
            require = 1
    )
    private static int subtractInnateFromGlobalLevel(int original, Player player) {
        LazyOptional<AptitudeCapability> aptitudeCapability = player.getCapability(com.seniors.justlevelingfork.registry.RegistryCapabilities.APTITUDE);

        int totalBonus = aptitudeCapability.map(cap ->
                cap.aptitudeLevel.keySet().stream()
                        .mapToInt(aptitudeName -> InnateAptitudeBonusPower.getBonus(player, aptitudeName))
                        .sum()
        ).orElse(0);

        return Math.max(original - totalBonus, 0);
    }

    @ModifyExpressionValue(
            method = "Ldev/muon/medieval/leveling/LevelingUtils;getPlayerLevelProgress(Lnet/minecraft/world/entity/player/Player;)D",
            at = @At(value = "INVOKE", target = "Lcom/seniors/justlevelingfork/common/capability/AptitudeCapability;getGlobalLevel()I"),
            require = 1
    )
    private static int subtractInnateFromGlobalProgress(int original, Player player) {
        LazyOptional<AptitudeCapability> aptitudeCapability = player.getCapability(com.seniors.justlevelingfork.registry.RegistryCapabilities.APTITUDE);

        int totalBonus = aptitudeCapability.map(cap ->
                cap.aptitudeLevel.keySet().stream()
                        .mapToInt(aptitudeName -> InnateAptitudeBonusPower.getBonus(player, aptitudeName))
                        .sum()
        ).orElse(0);

        return Math.max(original - totalBonus, 0);
    }

}
