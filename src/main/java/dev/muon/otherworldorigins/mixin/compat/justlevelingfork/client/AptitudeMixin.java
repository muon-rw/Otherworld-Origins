package dev.muon.otherworldorigins.mixin.compat.justlevelingfork.client;

import com.seniors.justlevelingfork.registry.aptitude.Aptitude;
import dev.muon.otherworldorigins.power.InnateAptitudeBonusPower;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;


@Mixin(value = Aptitude.class, remap = false)
public class AptitudeMixin {

    @ModifyVariable(method = "getLockedTexture()Lnet/minecraft/resources/ResourceLocation;", at = @At("STORE"), ordinal = 0)
    private int modifyAptitudeMaxLevel(int originalMaxLevel) {
        Aptitude self = (Aptitude) (Object) this;
        Player player = Minecraft.getInstance().player;
        if (player == null) return originalMaxLevel;

        int bonus = InnateAptitudeBonusPower.getBonus(player, self.getName());
        return originalMaxLevel + bonus;
    }

}