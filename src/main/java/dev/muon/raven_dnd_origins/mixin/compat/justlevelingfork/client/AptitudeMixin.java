package dev.muon.raven_dnd_origins.mixin.compat.justlevelingfork.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.seniors.justlevelingfork.registry.aptitude.Aptitude;
import dev.muon.raven_dnd_origins.power.InnateAptitudeBonusPower;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = Aptitude.class, remap = false)
public class AptitudeMixin {

    @ModifyExpressionValue(
            method = "getLockedTexture()Lnet/minecraft/resources/ResourceLocation;",
            at = @At(value = "FIELD", target = "Lcom/seniors/justlevelingfork/handler/HandlerCommonConfig;aptitudeMaxLevel:I", opcode = Opcodes.GETFIELD)
    )
    private int raiseMaxLevelByInnateBonus(int maxLevel) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return maxLevel;
        return maxLevel + InnateAptitudeBonusPower.getBonus(player, ((Aptitude) (Object) this).getName());
    }
}
