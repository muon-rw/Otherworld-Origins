package dev.muon.otherworldorigins.mixin.origins_patches;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.muon.otherworldorigins.config.OtherworldOriginsConfig;
import dev.muon.otherworldorigins.restrictions.EnchantmentRestrictions;
import io.github.apace100.origins.screen.OriginDisplayScreen;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.redspace.ironsspellbooks.IronsSpellbooks;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

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

    @Unique
    private MutableComponent appendCantripDesc(MutableComponent desc, String className) {
        // TODO: support other namespaces
        ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(IronsSpellbooks.MODID, className);

        Component spellDesc = Component.translatable("spell." + spellId.getNamespace() + "." + spellId.getPath() + ".guide")
                .withStyle(style -> style.withItalic(true));

        desc.append("\n\n").append(spellDesc);

        return desc;
    }

}
