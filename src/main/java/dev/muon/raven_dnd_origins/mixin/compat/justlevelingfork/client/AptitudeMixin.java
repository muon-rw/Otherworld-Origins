package dev.muon.raven_dnd_origins.mixin.compat.justlevelingfork.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.seniors.justlevelingfork.registry.aptitude.Aptitude;
import dev.muon.raven_dnd_origins.power.InnateAptitudeBonusPower;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = Aptitude.class, remap = false)
public class AptitudeMixin {

    @ModifyExpressionValue(
            method = "getLockedTexture()Lnet/minecraft/resources/ResourceLocation;",
            at = @At(value = "FIELD", target = "Lcom/seniors/justlevelingfork/handler/HandlerCommonConfig;aptitudeMaxLevel:I", opcode = Opcodes.GETFIELD)
    )
    private int raiseMaxLevelByInnateBonus(int maxLevel) {
        return maxLevel + raven_dnd_origins$innateBonus();
    }

    // This overload draws the icon for a requirement level, so the max must stay put for the tier
    // index; only the over-cap clamp it performs on the player's own level needs the bonus.
    @ModifyExpressionValue(
            method = "getLockedTexture(I)Lnet/minecraft/resources/ResourceLocation;",
            at = @At(value = "INVOKE", target = "Lcom/seniors/justlevelingfork/registry/aptitude/Aptitude;getLevel()I")
    )
    private int hideInnateBonusFromClamp(int level) {
        return level - raven_dnd_origins$innateBonus();
    }

    @Unique
    private int raven_dnd_origins$innateBonus() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return 0;
        return InnateAptitudeBonusPower.getBonus(player, ((Aptitude) (Object) this).getName());
    }
}
