package dev.muon.raven_dnd_origins.mixin.compat.justlevelingfork.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.seniors.justlevelingfork.client.core.Utils;
import com.seniors.justlevelingfork.client.screen.JustLevelingScreen;
import com.seniors.justlevelingfork.registry.RegistryAptitudes;
import com.seniors.justlevelingfork.registry.aptitude.Aptitude;
import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.network.RespecAptitudesMessage;
import dev.muon.raven_dnd_origins.power.InnateAptitudeBonusPower;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = JustLevelingScreen.class, remap = false)
public class JustLevelingScreenRespecMixin {

    @Unique
    private static final int RESPEC_COST = 10;
    @Unique
    private static final int REFUND_PERCENT = 50;
    @Unique
    private static final ResourceLocation RESPEC_BUTTON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RavenDndOrigins.MODID, "textures/gui/respec_button.png");

    @Unique
    private boolean raven_dnd_origins$confirmRespec = false;
    @Unique
    private boolean raven_dnd_origins$respecButtonHovered = false;

    @ModifyExpressionValue(
            method = "drawAptitudes",
            at = @At(value = "FIELD", target = "Lcom/seniors/justlevelingfork/handler/HandlerCommonConfig;aptitudeMaxLevel:I", opcode = Opcodes.GETFIELD)
    )
    private int raiseMaxLevelOnMainPage(int maxLevel, @Local(name = "aptitude") Aptitude aptitude) {
        return maxLevel + raven_dnd_origins$innateBonus(aptitude);
    }

    @ModifyExpressionValue(
            method = "drawSkills",
            at = @At(value = "FIELD", target = "Lcom/seniors/justlevelingfork/handler/HandlerCommonConfig;aptitudeMaxLevel:I", opcode = Opcodes.GETFIELD)
    )
    private int raiseMaxLevelOnSkillsPage(int maxLevel, @Local(name = "aptitude") Aptitude aptitude) {
        return maxLevel + raven_dnd_origins$innateBonus(aptitude);
    }

    @ModifyExpressionValue(
            method = "drawSkills",
            at = @At(value = "FIELD", target = "Lcom/seniors/justlevelingfork/handler/HandlerCommonConfig;playersMaxGlobalLevel:I", opcode = Opcodes.GETFIELD)
    )
    private int raiseGlobalMaxLevel(int maxLevel) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return maxLevel;
        int totalBonus = RegistryAptitudes.APTITUDES_REGISTRY.get().getValues().stream()
                .mapToInt(aptitude -> InnateAptitudeBonusPower.getBonus(player, aptitude.getName()))
                .sum();
        return maxLevel + totalBonus;
    }

    @Unique
    private int raven_dnd_origins$innateBonus(Aptitude aptitude) {
        Player player = Minecraft.getInstance().player;
        if (player == null || aptitude == null) return 0;
        return InnateAptitudeBonusPower.getBonus(player, aptitude.getName());
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void handleRespecButtonClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button != 0 || !raven_dnd_origins$respecButtonHovered) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.experienceLevel >= RESPEC_COST) {
            raven_dnd_origins$onRespecButtonClick();
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "drawAptitudes", at = @At("TAIL"))
    private void drawRespecButton(GuiGraphics matrixStack, int x, int y, int mouseX, int mouseY, CallbackInfo ci) {
        JustLevelingScreen screen = (JustLevelingScreen) (Object) this;
        int buttonWidth = 98;
        int buttonHeight = 20;
        int buttonX = x + 88 - buttonWidth / 2;
        int buttonY = y + 166 - buttonHeight + 24;

        Component buttonText = raven_dnd_origins$confirmRespec
                ? Component.translatable("button.raven_dnd_origins.respec_button_confirm")
                : Component.translatable("button.raven_dnd_origins.respec_button");

        LocalPlayer player = Minecraft.getInstance().player;
        boolean canAffordRespec = player != null && player.experienceLevel >= RESPEC_COST;

        raven_dnd_origins$respecButtonHovered = Utils.checkMouse(buttonX, buttonY, mouseX, mouseY, buttonWidth, buttonHeight);
        int textureY = 0;
        if (!canAffordRespec) {
            textureY = 40;
        } else if (raven_dnd_origins$respecButtonHovered) {
            textureY = 20;
        }

        matrixStack.blit(RESPEC_BUTTON_TEXTURE, buttonX, buttonY, 0, textureY, buttonWidth, buttonHeight, 98, 60);

        int textColor = canAffordRespec ? 0xFFFFFF : 0xA0A0A0;
        int textX = buttonX + (buttonWidth - screen.getMinecraft().font.width(buttonText)) / 2;
        int textY = buttonY + (buttonHeight - 8) / 2;
        matrixStack.drawString(screen.getMinecraft().font, buttonText, textX, textY, textColor);

        if (raven_dnd_origins$respecButtonHovered) {
            List<Component> tooltipLines = new ArrayList<>();
            if (!canAffordRespec) {
                tooltipLines.add(Component.translatable("tooltip.raven_dnd_origins.hover_not_enough_xp", RESPEC_COST));
            } else {
                if (raven_dnd_origins$confirmRespec) {
                    tooltipLines.add(Component.translatable("tooltip.raven_dnd_origins.hover_confirm").withStyle(ChatFormatting.BOLD));
                }
                tooltipLines.add(Component.translatable("tooltip.raven_dnd_origins.hover_cost", RESPEC_COST));
                tooltipLines.add(Component.translatable("tooltip.raven_dnd_origins.hover_refund", REFUND_PERCENT));
            }
            Utils.drawToolTipList(matrixStack, tooltipLines, mouseX, mouseY);
            screen.isMouseCheck = true;
        }
    }

    @Unique
    private void raven_dnd_origins$onRespecButtonClick() {
        if (!raven_dnd_origins$confirmRespec) {
            raven_dnd_origins$confirmRespec = true;
            return;
        }
        PacketDistributor.sendToServer(new RespecAptitudesMessage());
        raven_dnd_origins$confirmRespec = false;
    }
}
