package dev.muon.otherworldorigins.mixin.origins_patches;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.muon.otherworldorigins.config.OtherworldOriginsConfig;
import dev.muon.otherworldorigins.restrictions.EnchantmentRestrictions;
import io.github.apace100.origins.screen.OriginDisplayScreen;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.*;
import java.util.stream.Collectors;

@Mixin(value = OriginDisplayScreen.class, remap = false)
public abstract class OriginDisplayScreenMixin {

    @Shadow
    public abstract Holder<Origin> getCurrentOrigin();

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
        } else if (originPath.startsWith("cantrips/two/")) {
            modifiedDesc = appendCantripDesc(modifiedDesc, originPath.substring("cantrips/two/".length()));
        } else if (originPath.startsWith("cantrips/")) {
            modifiedDesc = appendCantripDesc(modifiedDesc, originPath.substring("cantrips/".length()));
        }

        return modifiedDesc;
    }

    @Unique
    private MutableComponent appendSubclassSpellAccess(MutableComponent desc, String path) {
        Map<String, List<String>> categoryRestrictions = OtherworldOriginsConfig.getClassRestrictions();
        Map<String, List<String>> schoolRestrictions = OtherworldOriginsConfig.getSchoolRestrictions();

        List<String> categories = categoryRestrictions.get(path);
        List<String> schools = schoolRestrictions.get(path);

        boolean hasAllCategories = categories != null &&
                new HashSet<>(categories).containsAll(Arrays.asList("OFFENSIVE", "SUPPORT", "DEFENSIVE"));
        boolean hasCategories = categories != null && !categories.isEmpty();
        boolean hasSchools = schools != null && !schools.isEmpty();

        // If has all 3 categories, just show "All spells" (schools are redundant)
        if (hasAllCategories) {
            desc.append("\n\n").append(
                    Component.translatable("otherworldorigins.gui.spell_access")
                            .withStyle(style -> style.withBold(true))
            );
            desc.append("\n").append(Component.translatable("otherworldorigins.gui.all_spells"));
            return desc;
        }

        // If neither categories nor schools, show "No spells"
        if (!hasCategories && !hasSchools) {
            desc.append("\n\n").append(
                    Component.translatable("otherworldorigins.gui.spell_access")
                            .withStyle(style -> style.withBold(true))
            );
            desc.append("\n").append(Component.translatable("otherworldorigins.gui.no_spells"));
            return desc;
        }

        // Add spell access header
        desc.append("\n\n").append(
                Component.translatable("otherworldorigins.gui.spell_access")
                        .withStyle(style -> style.withBold(true))
        );

        // Display category access
        if (hasCategories) {
            desc.append("\n").append(otherworld$formatCategoryAccess(categories));
        }

        // Display school access (colored by school type)
        if (hasSchools) {
            desc.append("\n").append(otherworld$formatSchoolAccess(schools));
        }

        return desc;
    }

    @Unique
    private Component otherworld$formatCategoryAccess(List<String> categories) {
        if (categories.size() == 1) {
            String cat = categories.get(0);
            return Component.translatable("otherworldorigins.gui.all_category_spells",
                    cat.substring(0, 1).toUpperCase() + cat.substring(1).toLowerCase());
        } else if (categories.size() == 2) {
            String cat1 = categories.get(0).substring(0, 1).toUpperCase() + categories.get(0).substring(1).toLowerCase();
            String cat2 = categories.get(1).substring(0, 1).toUpperCase() + categories.get(1).substring(1).toLowerCase();
            return Component.translatable("otherworldorigins.gui.all_two_category_spells", cat1, cat2);
        } else {
            // 3+ categories (but not all 3, since that case is handled earlier)
            String formatted = categories.stream()
                    .map(cat -> cat.substring(0, 1).toUpperCase() + cat.substring(1).toLowerCase())
                    .collect(Collectors.joining(", "));
            return Component.translatable("otherworldorigins.gui.all_category_spells", formatted);
        }
    }

    @Unique
    private Component otherworld$formatSchoolAccess(List<String> schoolIds) {
        List<SchoolType> schoolTypes = new ArrayList<>();
        for (String schoolId : schoolIds) {
            ResourceLocation loc = schoolId.contains(":")
                    ? ResourceLocation.tryParse(schoolId)
                    : ResourceLocation.fromNamespaceAndPath(IronsSpellbooks.MODID, schoolId);
            SchoolType schoolType = otherworld$getSchoolType(loc);
            if (schoolType != null) {
                schoolTypes.add(schoolType);
            }
        }

        if (schoolTypes.isEmpty()) {
            return Component.empty();
        }

        if (schoolTypes.size() == 1) {
            Component schoolName = otherworld$getStyledSchoolName(schoolTypes.get(0));
            return Component.translatable("otherworldorigins.gui.all_school_spells", schoolName);
        } else if (schoolTypes.size() == 2) {
            Component school1 = otherworld$getStyledSchoolName(schoolTypes.get(0));
            Component school2 = otherworld$getStyledSchoolName(schoolTypes.get(1));
            return Component.translatable("otherworldorigins.gui.all_two_school_spells", school1, school2);
        } else {
            // 3+ schools: build a combined component for the list
            MutableComponent schoolList = Component.empty();
            for (int i = 0; i < schoolTypes.size(); i++) {
                if (i > 0) {
                    if (i == schoolTypes.size() - 1) {
                        schoolList.append(Component.literal(" and "));
                    } else {
                        schoolList.append(Component.literal(", "));
                    }
                }
                schoolList.append(otherworld$getStyledSchoolName(schoolTypes.get(i)));
            }
            return Component.translatable("otherworldorigins.gui.all_school_spells", schoolList);
        }
    }

    @Unique
    private Component otherworld$getStyledSchoolName(SchoolType school) {
        Style schoolStyle = school.getDisplayName().getStyle();
        return Component.literal(otherworld$formatSchoolName(school)).withStyle(schoolStyle);
    }

    @Unique
    private String otherworld$formatSchoolName(SchoolType school) {
        String name = school.getId().getPath();
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }

    @Unique
    private SchoolType otherworld$getSchoolType(ResourceLocation id) {
        if (SchoolRegistry.REGISTRY.get() == null) return null;
        return SchoolRegistry.REGISTRY.get().getValue(id);
    }

    @Unique
    private MutableComponent appendClassSpellAccess(MutableComponent desc, String className) {
        Map<String, List<String>> categoryRestrictions = OtherworldOriginsConfig.getClassRestrictions();
        Map<String, List<String>> schoolRestrictions = OtherworldOriginsConfig.getSchoolRestrictions();

        Set<List<String>> subclassCategoryRestrictions = categoryRestrictions.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(className + "/"))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());

        Set<List<String>> subclassSchoolRestrictions = schoolRestrictions.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(className + "/"))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());

        if (subclassCategoryRestrictions.isEmpty() && subclassSchoolRestrictions.isEmpty()) return desc;

        // Add two line breaks and spell access header
        desc.append("\n\n").append(
                Component.translatable("otherworldorigins.gui.spell_access")
                        .withStyle(style -> style.withUnderlined(true).withColor(13434879))
        );

        // Check if all subclasses have no spell access (no categories AND no schools)
        boolean allNoSpells = subclassCategoryRestrictions.stream()
                .allMatch(list -> list == null || list.isEmpty()) &&
                subclassSchoolRestrictions.stream()
                        .allMatch(list -> list == null || list.isEmpty());

        // Check if all subclasses have all 3 categories (schools don't matter then)
        boolean allAllSpells = subclassCategoryRestrictions.stream()
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
                    enchantmentName).withStyle(style -> style.withColor(16738047));
            ;
            desc.append("\n").append(Component.literal("• ")).append(fullMessage);
        }

        return desc;
    }

    @Unique
    private MutableComponent appendCantripDesc(MutableComponent desc, String className) {

        // TODO: Implement support for other namespaces, will need to add to codec and parse power data
        ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(IronsSpellbooks.MODID, className);
        AbstractSpell spell = SpellRegistry.getSpell(spellId);

        Component spellDesc = Component.translatable("spell." + spellId.getNamespace() + "." + spellId.getPath() + ".guide")
                .withStyle(style -> style.withItalic(true));

        desc.append("\n\n").append(spellDesc);

        return desc;
    }

}