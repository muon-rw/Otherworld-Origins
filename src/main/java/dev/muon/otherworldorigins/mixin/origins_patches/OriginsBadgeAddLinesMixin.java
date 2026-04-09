package dev.muon.otherworldorigins.mixin.origins_patches;

import dev.muon.otherworldorigins.client.BadgeTooltipLinebreaks;
import io.github.apace100.origins.badge.KeybindBadge;
import io.github.apace100.origins.badge.TooltipBadge;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = { TooltipBadge.class, KeybindBadge.class }, remap = false)
public class OriginsBadgeAddLinesMixin {

    @Inject(method = "addLines", at = @At("HEAD"), cancellable = true)
    private static void otherworldorigins$splitExplicitNewlines(
            List<ClientTooltipComponent> tooltips,
            Component text,
            Font textRenderer,
            int widthLimit,
            CallbackInfo ci
    ) {
        BadgeTooltipLinebreaks.addLines(tooltips, text, textRenderer, widthLimit);
        ci.cancel();
    }
}
