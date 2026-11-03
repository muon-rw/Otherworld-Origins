package dev.muon.raven_dnd_origins.mixin.origins_patches;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.muon.raven_dnd_origins.config.RavenDndOriginsConfig;
import dev.muon.raven_dnd_origins.restrictions.EnchantmentRestrictions;
import dev.muon.raven_dnd_origins.util.ElementalDisciplineSpellDisplay;
import dev.overgrown.apoli.data.IconData;
import dev.overgrown.origins.client.screen.OriginDisplayScreen;
import dev.overgrown.origins.origin.Origin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.Optional;

@Mixin(value = OriginDisplayScreen.class, remap = false)
public abstract class OriginDisplayScreenMixin {

    @Shadow
    public abstract Origin getCurrentOrigin();

    @ModifyExpressionValue(method = "renderOriginContent", at = @At(value = "INVOKE", ordinal = 0, target = "Ldev/overgrown/origins/origin/Origin;description()Lnet/minecraft/network/chat/Component;"))
    private Component appendExtraInfo(Component orgDesc) {
        Origin currentOrigin = this.getCurrentOrigin();
        if (currentOrigin == null) return orgDesc;

        String originPath = currentOrigin.id().getPath();
        MutableComponent modifiedDesc = orgDesc.copy();

        if (originPath.startsWith("class/")) {
            modifiedDesc = raven_dnd_origins$appendEnchantmentAccess(modifiedDesc, originPath.substring("class/".length()));
        } else {
            Optional<ResourceLocation> spell = ElementalDisciplineSpellDisplay.spellIdForCantripOriginPath(originPath)
                    .or(() -> ElementalDisciplineSpellDisplay.spellIdForDisciplineOriginPath(originPath));
            if (spell.isPresent()) {
                modifiedDesc = ElementalDisciplineSpellDisplay.appendSpellGuide(modifiedDesc, spell.get());
            }
        }

        return modifiedDesc;
    }

    @Unique
    private MutableComponent raven_dnd_origins$appendEnchantmentAccess(MutableComponent desc, String className) {
        if (!RavenDndOriginsConfig.enableEnchantmentRestrictions()) {
            return desc;
        }

        List<Component> classEnchantments = EnchantmentRestrictions.getEnchantmentTextForClass(className);
        if (classEnchantments.isEmpty()) {
            return desc;
        }

        desc.append("\n\n").append(
                Component.translatable("raven_dnd_origins.gui.enchantment_access")
                        .withStyle(style -> style.withUnderlined(true).withColor(16738047))
        );

        String formattedClass = className.substring(0, 1).toUpperCase() +
                className.substring(1).toLowerCase() + "s";

        for (Component enchantmentName : classEnchantments) {
            Component fullMessage = Component.translatable("raven_dnd_origins.gui.enchantment_restriction",
                    formattedClass,
                    enchantmentName).withStyle(style -> style.withColor(16738047));
            desc.append("\n").append(Component.literal("• ")).append(fullMessage);
        }

        return desc;
    }

    @WrapOperation(
            method = "renderOriginHeader",
            at = @At(value = "INVOKE", target = "Ldev/overgrown/apoli/client/IconRenderer;render(Lnet/minecraft/client/gui/GuiGraphics;Ldev/overgrown/apoli/data/IconData;II)V")
    )
    private void raven_dnd_origins$renderCantripSpellIcon(GuiGraphics graphics, IconData icon, int x, int y, Operation<Void> original) {
        Origin currentOrigin = this.getCurrentOrigin();
        if (currentOrigin != null) {
            String originPath = currentOrigin.id().getPath();
            Optional<ResourceLocation> spell = ElementalDisciplineSpellDisplay.spellIdForCantripOriginPath(originPath)
                    .or(() -> ElementalDisciplineSpellDisplay.spellIdForDisciplineOriginPath(originPath));
            if (spell.isPresent()) {
                ResourceLocation id = spell.get();
                ResourceLocation iconTexture = ResourceLocation.fromNamespaceAndPath(
                        id.getNamespace(), "textures/gui/spell_icons/" + id.getPath() + ".png");
                graphics.blit(iconTexture, x, y, 0, 0, 16, 16, 16, 16);
                return;
            }
        }
        original.call(graphics, icon, x, y);
    }
}
