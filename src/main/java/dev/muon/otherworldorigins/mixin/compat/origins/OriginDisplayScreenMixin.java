package dev.muon.otherworldorigins.mixin.compat.origins;

import dev.muon.otherworldorigins.config.OtherworldOriginsConfig;
import io.github.apace100.origins.screen.OriginDisplayScreen;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.stream.Collectors;

@Mixin(OriginDisplayScreen.class)
public class OriginDisplayScreenMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void renderSpellRestrictions(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        OriginDisplayScreen screen = (OriginDisplayScreen) (Object) this;
        Holder<Origin> currentOrigin = screen.getCurrentOrigin();

        if (!currentOrigin.isBound()) return;

        String originPath = currentOrigin.unwrapKey()
                .map(key -> key.location().getPath())
                .orElse("");

        int centerX = screen.width / 2;
        int centerY = screen.height / 2;
        int x = centerX + 94;
        int y = centerY - 60;

        if (originPath.startsWith("subclass/")) {
            renderSubclassSpellAccess(graphics, screen, originPath.substring("subclass/".length()), x, y);
        } else if (originPath.startsWith("class/")) {
            renderClassSpellAccess(graphics, screen, originPath.substring("class/".length()), x, y);
        }
    }

    private void renderSubclassSpellAccess(GuiGraphics graphics, OriginDisplayScreen screen, String path, int x, int y) {
        Map<String, List<String>> restrictions = OtherworldOriginsConfig.getClassRestrictions();
        List<String> categories = restrictions.get(path);

        graphics.drawString(screen.getMinecraft().font,
                Component.translatable("otherworldorigins.gui.spell_access")
                        .withStyle(style -> style.withBold(true)),
                x, y, 0xFFFFFF);

        y += 15;

        if (categories == null || categories.isEmpty()) {
            graphics.drawString(screen.getMinecraft().font,
                    Component.translatable("otherworldorigins.gui.no_spells"),
                    x + 5, y, 0xE0E0E0);
            return;
        }

        if (new HashSet<>(categories).containsAll(Arrays.asList("OFFENSIVE", "SUPPORT", "DEFENSIVE"))) {
            graphics.drawString(screen.getMinecraft().font,
                    Component.translatable("otherworldorigins.gui.all_spells"),
                    x + 5, y, 0xE0E0E0);
            return;
        }

        for (String category : categories) {
            graphics.drawString(screen.getMinecraft().font,
                    Component.literal("• " + category.substring(0, 1).toUpperCase() +
                            category.substring(1).toLowerCase()),
                    x + 5, y, 0xE0E0E0);
            y += 12;
        }
    }

    private void renderClassSpellAccess(GuiGraphics graphics, OriginDisplayScreen screen, String className, int x, int y) {
        Map<String, List<String>> restrictions = OtherworldOriginsConfig.getClassRestrictions();

        Set<List<String>> subclassRestrictions = restrictions.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(className + "/"))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());

        if (subclassRestrictions.isEmpty()) return;

        graphics.drawString(screen.getMinecraft().font,
                Component.translatable("otherworldorigins.gui.spell_access")
                        .withStyle(style -> style.withBold(true)),
                x, y, 0xFFFFFF);

        y += 15;

        boolean allNoSpells = subclassRestrictions.stream()
                .allMatch(list -> list == null || list.isEmpty());

        boolean allAllSpells = subclassRestrictions.stream()
                .allMatch(list -> list != null &&
                        new HashSet<>(list).containsAll(Arrays.asList("OFFENSIVE", "SUPPORT", "DEFENSIVE")));

        Component message;
        if (allNoSpells) {
            message = Component.translatable("otherworldorigins.gui.no_spells");
        } else if (allAllSpells) {
            message = Component.translatable("otherworldorigins.gui.all_spells");
        } else {
            message = Component.translatable("otherworldorigins.gui.varies");
        }

        graphics.drawString(screen.getMinecraft().font, message, x + 5, y, 0xE0E0E0);
    }
}