package dev.muon.otherworldorigins.mixin.origins_patches;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.muon.otherworldorigins.config.OtherworldOriginsConfig;
import dev.muon.otherworldorigins.restrictions.EnchantmentRestrictions;
import io.github.apace100.origins.screen.OriginDisplayScreen;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.*;
import java.util.stream.Collectors;

@Mixin(value = OriginDisplayScreen.class, remap = false)
public abstract class OriginDisplayScreenMixin {
    
    @Shadow public abstract Holder<Origin> getCurrentOrigin();
    
    @ModifyExpressionValue(method = "renderOriginContent", at = @At(value = "INVOKE", ordinal = 0, target = "Lio/github/edwinmindcraft/origins/api/origin/Origin;getDescription()Lnet/minecraft/network/chat/Component;"))
    private Component appendSpellAndEnchantmentInfo(Component orgDesc) {
        Holder<Origin> currentOrigin = this.getCurrentOrigin();
        
        if (!currentOrigin.isBound()) return orgDesc;
        
        String originPath = currentOrigin.unwrapKey()
                .map(key -> key.location().getPath())
                .orElse("");
        
        MutableComponent modifiedDesc = orgDesc.copy();
        
        if (originPath.startsWith("subclass/")) {
            modifiedDesc = appendSubclassSpellAccess(modifiedDesc, originPath.substring("subclass/".length()));
        } else if (originPath.startsWith("class/")) {
            modifiedDesc = appendClassSpellAccess(modifiedDesc, originPath.substring("class/".length()));
            modifiedDesc = appendEnchantmentAccess(modifiedDesc, originPath.substring("class/".length()));
        }
        
        return modifiedDesc;
    }

    @Unique
    private MutableComponent appendSubclassSpellAccess(MutableComponent desc, String path) {
        Map<String, List<String>> restrictions = OtherworldOriginsConfig.getClassRestrictions();
        List<String> categories = restrictions.get(path);

        // Add two line breaks and spell access header
        desc.append("\n\n").append(
                Component.translatable("otherworldorigins.gui.spell_access")
                        .withStyle(style -> style.withBold(true))
        );

        if (categories == null || categories.isEmpty()) {
            desc.append("\n").append(Component.translatable("otherworldorigins.gui.no_spells"));
            return desc;
        }

        if (new HashSet<>(categories).containsAll(Arrays.asList("OFFENSIVE", "SUPPORT", "DEFENSIVE"))) {
            desc.append("\n").append(Component.translatable("otherworldorigins.gui.all_spells"));
            return desc;
        }

        for (String category : categories) {
            desc.append("\n").append(Component.literal("• " + category.substring(0, 1).toUpperCase() +
                    category.substring(1).toLowerCase()));
        }
        
        return desc;
    }

    @Unique
    private MutableComponent appendClassSpellAccess(MutableComponent desc, String className) {
        Map<String, List<String>> restrictions = OtherworldOriginsConfig.getClassRestrictions();

        Set<List<String>> subclassRestrictions = restrictions.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(className + "/"))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());

        if (subclassRestrictions.isEmpty()) return desc;

        // Add two line breaks and spell access header
        desc.append("\n\n").append(
                Component.translatable("otherworldorigins.gui.spell_access")
                        .withStyle(style -> style.withUnderlined(true).withColor(13434879))
        );

        boolean allNoSpells = subclassRestrictions.stream()
                .allMatch(list -> list == null || list.isEmpty());

        boolean allAllSpells = subclassRestrictions.stream()
                .allMatch(list -> list != null &&
                        new HashSet<>(list).containsAll(Arrays.asList("OFFENSIVE", "SUPPORT", "DEFENSIVE")));

        Component message;
        if (allNoSpells) {
            message = Component.translatable("otherworldorigins.gui.no_spells").withStyle(style -> style.withColor(13434879));
        } else if (allAllSpells) {
            message = Component.translatable("otherworldorigins.gui.all_spells").withStyle(style -> style.withColor(13434879));
        } else {
            message = Component.translatable("otherworldorigins.gui.varies").withStyle(style -> style.withColor(13434879));
        }

        desc.append("\n").append(message);
        return desc;
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

        // Add two line breaks and enchantment access header
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
                    enchantmentName).withStyle(style -> style.withColor(16738047));;
            desc.append("\n").append(Component.literal("• ")).append(fullMessage);
        }
        
        return desc;
    }

}