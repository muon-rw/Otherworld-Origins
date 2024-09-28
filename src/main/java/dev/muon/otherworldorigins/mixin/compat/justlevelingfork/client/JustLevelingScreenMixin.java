package dev.muon.otherworldorigins.mixin.compat.justlevelingfork.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.seniors.justlevelingfork.client.screen.JustLevelingScreen;
import com.seniors.justlevelingfork.registry.aptitude.Aptitude;
import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.power.configuration.InnateAptitudeBonusConfiguration;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = JustLevelingScreen.class, remap = false)
public class JustLevelingScreenMixin {
    @Unique
    private Aptitude otherworld$currentAptitude;

    @ModifyExpressionValue(
            method = {"drawSkills", "drawAptitudes"},
            at = @At(value = "FIELD",
                    target = "Lcom/seniors/justlevelingfork/handler/HandlerCommonConfig;aptitudeMaxLevel:I")
    )
    private int modifyAptitudeMaxLevel(int originalMaxLevel) {
        if (this.otherworld$currentAptitude == null) {
            return originalMaxLevel;
        }

        String aptitudeName = this.otherworld$currentAptitude.getName();
        int bonus = InnateAptitudeBonusConfiguration.getBonus(Minecraft.getInstance().player, aptitudeName);
        //int newMaxLevel = originalMaxLevel + bonus;
        //OtherworldOrigins.LOGGER.info("Aptitude: {}, Original max level: {}, Bonus: {}, New max level: {}", aptitudeName, originalMaxLevel, bonus, newMaxLevel);
        return originalMaxLevel + bonus;
    }

    @WrapOperation(
            method = "drawAptitudes",
            at = @At(value = "INVOKE",
                    target = "Lcom/seniors/justlevelingfork/registry/aptitude/Aptitude;getKey()Ljava/lang/String;")
    )
    private String captureCurrentAptitude(Aptitude aptitude, Operation<String> original) {
        this.otherworld$currentAptitude = aptitude;
        return original.call(aptitude);
    }

    @WrapOperation(
            method = "drawAptitudes",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;")
    )
    private MutableComponent modifyExperienceText(String key, Object[] args, Operation<MutableComponent> original) {
        if ("screen.aptitude.experience".equals(key) && args.length == 2 && this.otherworld$currentAptitude != null) {
            Object aptitudeLevelObj = args[0];
            Object originalMaxLevelObj = args[1];

            if (aptitudeLevelObj instanceof String && originalMaxLevelObj instanceof String) {
                String aptitudeLevelStr = (String) aptitudeLevelObj;
                String originalMaxLevelStr = (String) originalMaxLevelObj;

                try {
                    int aptitudeLevel = Integer.parseInt(aptitudeLevelStr);
                    int originalMaxLevel = Integer.parseInt(originalMaxLevelStr);

                    int bonus = InnateAptitudeBonusConfiguration.getBonus(Minecraft.getInstance().player, this.otherworld$currentAptitude.getName());
                    int modifiedMaxLevel = originalMaxLevel + bonus;

                    // Create new args array with modified maxLevel
                    Object[] newArgs = new Object[]{aptitudeLevelStr, String.valueOf(modifiedMaxLevel)};
                    return original.call(key, newArgs);
                } catch (NumberFormatException e) {
                    OtherworldOrigins.LOGGER.error("Error parsing aptitude level or max level: {}, {}", aptitudeLevelStr, originalMaxLevelStr, e);
                }
            }
        }
        return original.call(key, args);
    }
}