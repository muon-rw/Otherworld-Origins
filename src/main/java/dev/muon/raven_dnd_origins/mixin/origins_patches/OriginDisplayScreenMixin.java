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
import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
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
        } else if (originPath.startsWith("cantrips/two/")) {
            modifiedDesc = raven_dnd_origins$appendCantripDesc(modifiedDesc, originPath.substring("cantrips/two/".length()));
        } else if (originPath.startsWith("cantrips/magical_secrets/")) {
            modifiedDesc = raven_dnd_origins$appendCantripDesc(modifiedDesc, originPath.substring("cantrips/magical_secrets/".length()));
        } else if (originPath.startsWith("cantrips/")) {
            modifiedDesc = raven_dnd_origins$appendCantripDesc(modifiedDesc, originPath.substring("cantrips/".length()));
        } else {
            Optional<ResourceLocation> disciplineSpell = ElementalDisciplineSpellDisplay.spellIdForDisciplineOriginPath(originPath);
            if (disciplineSpell.isPresent()) {
                modifiedDesc = ElementalDisciplineSpellDisplay.appendSpellGuide(modifiedDesc, disciplineSpell.get());
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

    /**
     * Resolves a spell's mod namespace from just its path name (e.g. "ashen_breath").
     * The origin display screen only has the Origin id (e.g. "cantrips/two/ashen_breath"),
     * not the actual spell object; that's buried in the power's entity action config, which would
     * require parsing serialized JSON from the Origin to Power to ActionConfig chain. Instead, we scan
     * the ISS spell registry by path. If this ever becomes insufficient (e.g. cross-mod path
     * collisions), alternatives include namespaced origin subdirectories or a manual override map.
     */
    @Unique
    private static String raven_dnd_origins$resolveNamespace(String spellName) {
        for (AbstractSpell spell : SpellRegistry.REGISTRY) {
            if (spell.getSpellResource().getPath().equals(spellName)) {
                return spell.getSpellResource().getNamespace();
            }
        }
        return IronsSpellbooks.MODID;
    }

    @Unique
    private MutableComponent raven_dnd_origins$appendCantripDesc(MutableComponent desc, String spellName) {
        String namespace = raven_dnd_origins$resolveNamespace(spellName);
        ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(namespace, spellName);

        Component spellDesc = Component.translatable("spell." + spellId.getNamespace() + "." + spellId.getPath() + ".guide")
                .withStyle(style -> style.withItalic(true));

        desc.append("\n\n").append(spellDesc);

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

            String spellName = null;
            if (originPath.startsWith("cantrips/two/")) {
                spellName = originPath.substring("cantrips/two/".length());
            } else if (originPath.startsWith("cantrips/magical_secrets/")) {
                spellName = originPath.substring("cantrips/magical_secrets/".length());
            } else if (originPath.startsWith("cantrips/")) {
                spellName = originPath.substring("cantrips/".length());
            }

            if (spellName != null) {
                String namespace = raven_dnd_origins$resolveNamespace(spellName);
                ResourceLocation iconTexture = ResourceLocation.fromNamespaceAndPath(
                        namespace, "textures/gui/spell_icons/" + spellName + ".png");
                graphics.blit(iconTexture, x, y, 0, 0, 16, 16, 16, 16);
                return;
            }

            Optional<ResourceLocation> disciplineSpell = ElementalDisciplineSpellDisplay.spellIdForDisciplineOriginPath(originPath);
            if (disciplineSpell.isPresent()) {
                ResourceLocation id = disciplineSpell.get();
                ResourceLocation iconTexture = ResourceLocation.fromNamespaceAndPath(
                        id.getNamespace(), "textures/gui/spell_icons/" + id.getPath() + ".png");
                graphics.blit(iconTexture, x, y, 0, 0, 16, 16, 16, 16);
                return;
            }
        }
        original.call(graphics, icon, x, y);
    }
}
