package dev.muon.otherworldorigins.mixin.compat.justlevelingfork.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.seniors.justlevelingfork.client.core.Utils;
import com.seniors.justlevelingfork.client.screen.JustLevelingScreen;
import com.seniors.justlevelingfork.registry.aptitude.Aptitude;
import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.network.CheckFeatScreenMessage;
import dev.muon.otherworldorigins.network.RespecAptitudesMessage;
import dev.muon.otherworldorigins.power.InnateAptitudeBonusPower;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = JustLevelingScreen.class, remap = false)
public class JustLevelingScreenMixin {
    @Unique
    private Aptitude otherworld$iteratingAptitude;

    @Unique
    private Aptitude otherworld$selectedAptitude;

    @ModifyExpressionValue(
            method = "drawAptitudes",
            at = @At(value = "FIELD",
                    target = "Lcom/seniors/justlevelingfork/handler/HandlerCommonConfig;aptitudeMaxLevel:I")
    )
    private int modifyMaxLevelMainPage(int originalMaxLevel) {
        if (this.otherworld$iteratingAptitude == null) {
            return originalMaxLevel;
        }
        return otherworld$getModifiedMaxLevel(originalMaxLevel, this.otherworld$iteratingAptitude);
    }

    @ModifyExpressionValue(
            method = "drawSkills",
            at = @At(value = "FIELD",
                    target = "Lcom/seniors/justlevelingfork/handler/HandlerCommonConfig;aptitudeMaxLevel:I")
    )
    private int modifyMaxLevelSkillsPage(int originalMaxLevel) {
        if (this.otherworld$selectedAptitude == null) {
            return originalMaxLevel;
        }
        return otherworld$getModifiedMaxLevel(originalMaxLevel, this.otherworld$selectedAptitude);
    }

    @Unique
    private int otherworld$getModifiedMaxLevel(int originalMaxLevel, Aptitude aptitude) {
        String aptitudeName = aptitude.getName();
        int bonus = InnateAptitudeBonusPower.getBonus(Minecraft.getInstance().player, aptitudeName);
        int newMaxLevel = originalMaxLevel + bonus;
        //OtherworldOrigins.LOGGER.info("Aptitude: {}, Original max level: {}, Bonus: {}, New max level: {}", aptitudeName, originalMaxLevel, bonus, newMaxLevel);
        return newMaxLevel;
    }


    @WrapOperation(
            method = "drawAptitudes",
            at = @At(value = "INVOKE",
                    target = "Lcom/seniors/justlevelingfork/registry/aptitude/Aptitude;getKey()Ljava/lang/String;")
    )
    private String captureCurrentAptitude(Aptitude aptitude, Operation<String> original) {
        this.otherworld$iteratingAptitude = aptitude;
        return original.call(aptitude);
    }

    @WrapOperation(
            method = "drawSkills",
            at = @At(value = "INVOKE",
                    target = "Lcom/seniors/justlevelingfork/registry/RegistryAptitudes;getAptitude(Ljava/lang/String;)Lcom/seniors/justlevelingfork/registry/aptitude/Aptitude;")
    )
    private Aptitude captureSelectedAptitude(String selectedAptitude, Operation<Aptitude> original) {
        Aptitude aptitude = original.call(selectedAptitude);
        this.otherworld$selectedAptitude = aptitude;
        return aptitude;
    }

    @Inject(method = "drawSkills", at = @At(value = "INVOKE", target = "Lcom/seniors/justlevelingfork/network/packet/common/AptitudeLevelUpSP;send(Lcom/seniors/justlevelingfork/registry/aptitude/Aptitude;)V"))
    private void onAptitudeLevelUp(GuiGraphics matrixStack, int x, int y, int mouseX, int mouseY, CallbackInfo ci) {
        LocalPlayer player = ((JustLevelingScreen) (Object) this).getMinecraft().player;
        if (player != null) {
            OtherworldOrigins.CHANNEL.sendToServer(new CheckFeatScreenMessage());
        }

    }


    @Unique
    private static final int RESPEC_COST = 10;
    @Unique
    private static final int REFUND_PERCENT = 50;
    @Unique
    private boolean otherworld$confirmRespec = false;

    @Unique
    private static final ResourceLocation RESPEC_BUTTON_TEXTURE = new ResourceLocation(OtherworldOrigins.MODID, "textures/gui/respec_button.png");

    @Unique
    private boolean otherworld$respecButtonHovered = false;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, remap = true /* ?????? */)
    private void handleRespecButtonClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button == 0 && otherworld$respecButtonHovered) {
            LocalPlayer player = Minecraft.getInstance().player;
            boolean canAffordRespec = player != null && player.experienceLevel >= 10;
            if (canAffordRespec) {
                otherworld$onRespecButtonClick();
                cir.setReturnValue(true);
                cir.cancel();
            }
        }
    }

    @Inject(method = "drawAptitudes", at = @At("TAIL"))
    private void addRespecButton(GuiGraphics matrixStack, int x, int y, int mouseX, int mouseY, CallbackInfo ci) {
        JustLevelingScreen screen = (JustLevelingScreen) (Object) this;
        int buttonWidth = 98;
        int buttonHeight = 20;
        int buttonX = x + 88 - buttonWidth / 2;
        int buttonY = y + 166 - buttonHeight + 24;

        Component buttonText = otherworld$confirmRespec
                ? Component.translatable("button.otherworldorigins.respec_button_confirm")
                : Component.translatable("button.otherworldorigins.respec_button");

        LocalPlayer player = Minecraft.getInstance().player;
        boolean canAffordRespec = player != null && player.experienceLevel >= RESPEC_COST;

        int textureY = 0;
        otherworld$respecButtonHovered = Utils.checkMouse(buttonX, buttonY, mouseX, mouseY, buttonWidth, buttonHeight);
        if (!canAffordRespec) {
            textureY = 40;
        } else if (otherworld$respecButtonHovered) {
            textureY = 20;
        }

        RenderSystem.setShaderTexture(0, RESPEC_BUTTON_TEXTURE);
        matrixStack.blit(RESPEC_BUTTON_TEXTURE, buttonX, buttonY, 0, textureY, buttonWidth, buttonHeight, 98, 60);

        int textColor = canAffordRespec ? 0xFFFFFF : 0xA0A0A0;
        int textX = buttonX + (buttonWidth - screen.getMinecraft().font.width(buttonText)) / 2;
        int textY = buttonY + (buttonHeight - 8) / 2;
        matrixStack.drawString(screen.getMinecraft().font, buttonText, textX, textY, textColor);

        if (otherworld$respecButtonHovered) {
            List<Component> tooltipLines = new ArrayList<>();
            if (!canAffordRespec) {
                tooltipLines.add(Component.translatable("tooltip.otherworldorigins.hover_not_enough_xp", RESPEC_COST));
            } else {
                if (otherworld$confirmRespec) {
                    tooltipLines.add(Component.translatable("tooltip.otherworldorigins.hover_confirm").withStyle(ChatFormatting.BOLD));
                }
                tooltipLines.add(Component.translatable("tooltip.otherworldorigins.hover_cost", RESPEC_COST));
                tooltipLines.add(Component.translatable("tooltip.otherworldorigins.hover_refund", REFUND_PERCENT));
            }
            Utils.drawToolTipList(matrixStack, tooltipLines, mouseX, mouseY);
        }

        if (otherworld$respecButtonHovered) {
            screen.isMouseCheck = true;
        }
    }

    @Unique
    private void otherworld$onRespecButtonClick() {
        OtherworldOrigins.LOGGER.debug("Respec button clicked");
        if (!otherworld$confirmRespec) {
            otherworld$confirmRespec = true;
            OtherworldOrigins.LOGGER.debug("Confirm respec enabled");
            return;
        }

        RespecAptitudesMessage.send();
        otherworld$confirmRespec = false;
        OtherworldOrigins.LOGGER.debug("Respec message sent to server");
    }
}