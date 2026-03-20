package dev.muon.otherworldorigins.mixin.origins_patches;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.muon.otherworldorigins.config.OtherworldOriginsConfig;
import dev.muon.otherworldorigins.restrictions.EnchantmentRestrictions;
import io.github.apace100.origins.screen.OriginDisplayScreen;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.redspace.ironsspellbooks.IronsSpellbooks;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;

import java.util.*;

@Mixin(value = OriginDisplayScreen.class, remap = false)
public abstract class OriginDisplayScreenMixin {

    @Shadow
    public abstract Holder<Origin> getCurrentOrigin();

    @ModifyExpressionValue(method = "renderOriginContent", at = @At(value = "INVOKE", ordinal = 0, target = "Lio/github/edwinmindcraft/origins/api/origin/Origin;getDescription()Lnet/minecraft/network/chat/Component;"))
    private Component appendExtraInfo(Component orgDesc) {
        Holder<Origin> currentOrigin = this.getCurrentOrigin();

        if (!currentOrigin.isBound()) return orgDesc;

        String originPath = currentOrigin.unwrapKey()
                .map(key -> key.location().getPath())
                .orElse("");

        MutableComponent modifiedDesc = orgDesc.copy();

        if (originPath.startsWith("class/")) {
            modifiedDesc = appendEnchantmentAccess(modifiedDesc, originPath.substring("class/".length()));
        } else if (originPath.startsWith("cantrips/two/")) {
            modifiedDesc = appendCantripDesc(modifiedDesc, originPath.substring("cantrips/two/".length()));
        } else if (originPath.startsWith("cantrips/")) {
            modifiedDesc = appendCantripDesc(modifiedDesc, originPath.substring("cantrips/".length()));
        }

        return modifiedDesc;
    }

    @Unique
    private MutableComponent appendEnchantmentAccess(MutableComponent desc, String className) {
        if (!OtherworldOriginsConfig.ENABLE_ENCHANTMENT_RESTRICTIONS.get()) {
            return desc;
        }

        List<Enchantment> classEnchantments = EnchantmentRestrictions.getEnchantmentTextForClass(className);
        if (classEnchantments.isEmpty()) {
            return desc;
        }

        desc.append("\n\n").append(
                Component.translatable("otherworldorigins.gui.enchantment_access")
                        .withStyle(style -> style.withUnderlined(true).withColor(16738047))
        );

        String formattedClass = className.substring(0, 1).toUpperCase() +
                className.substring(1).toLowerCase() + "s";

        for (Enchantment enchantment : classEnchantments) {
            Component enchantmentName = Component.translatable(enchantment.getDescriptionId());
            Component fullMessage = Component.translatable("otherworldorigins.gui.enchantment_restriction",
                    formattedClass,
                    enchantmentName).withStyle(style -> style.withColor(16738047));
            desc.append("\n").append(Component.literal("• ")).append(fullMessage);
        }

        return desc;
    }

    /**
     * Resolves a spell's mod namespace from just its path name (e.g. "ashen_breath" → "traveloptics").
     * The origin display screen only has the Origin registry key (e.g. "cantrips/two/ashen_breath"),
     * not the actual spell object — that's buried in the power's entity action config, which would
     * require parsing serialized JSON from the Origin→Power→ActionConfig chain (insanely dumb). Instead, we scan
     * the ISS spell registry by path. If this ever becomes insufficient (e.g. cross-mod path
     * collisions), alternatives include namespaced origin subdirectories or a manual override map.
     */
    @Unique
    private static String otherworldorigins$resolveNamespace(String spellName) {
        for (AbstractSpell spell : SpellRegistry.REGISTRY.get().getValues()) {
            if (spell.getSpellResource().getPath().equals(spellName)) {
                return spell.getSpellResource().getNamespace();
            }
        }
        return IronsSpellbooks.MODID;
    }

    @Unique
    private MutableComponent appendCantripDesc(MutableComponent desc, String spellName) {
        String namespace = otherworldorigins$resolveNamespace(spellName);
        ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(namespace, spellName);

        Component spellDesc = Component.translatable("spell." + spellId.getNamespace() + "." + spellId.getPath() + ".guide")
                .withStyle(style -> style.withItalic(true));

        desc.append("\n\n").append(spellDesc);

        return desc;
    }

    @WrapOperation(
            method = "renderOriginName",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderItem(Lnet/minecraft/world/item/ItemStack;II)V")
    )
    private void otherworldorigins$renderCantripSpellIcon(GuiGraphics graphics, ItemStack stack, int x, int y, Operation<Void> original) {
        Holder<Origin> currentOrigin = this.getCurrentOrigin();
        if (currentOrigin.isBound()) {
            String originPath = currentOrigin.unwrapKey()
                    .map(key -> key.location().getPath())
                    .orElse("");

            String spellName = null;
            if (originPath.startsWith("cantrips/two/")) {
                spellName = originPath.substring("cantrips/two/".length());
            } else if (originPath.startsWith("cantrips/")) {
                spellName = originPath.substring("cantrips/".length());
            }

            if (spellName != null) {
                String namespace = otherworldorigins$resolveNamespace(spellName);
                ResourceLocation iconTexture = ResourceLocation.fromNamespaceAndPath(
                        namespace, "textures/gui/spell_icons/" + spellName + ".png");
                graphics.blit(iconTexture, x, y, 0, 0, 16, 16, 16, 16);
                return;
            }
        }
        original.call(graphics, stack, x, y);
    }

}
