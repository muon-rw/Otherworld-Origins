package dev.muon.otherworldorigins.config;

import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.config.ConfigSection;
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedList;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;

public class OtherworldOriginsConfig extends Config {
    private static OtherworldOriginsConfig INSTANCE;

    public OtherworldOriginsConfig() {
        super(ResourceLocation.fromNamespaceAndPath("otherworldorigins", "config"));
    }

    public static void setInstance(OtherworldOriginsConfig instance) {
        INSTANCE = instance;
    }

    public static OtherworldOriginsConfig getInstance() {
        return INSTANCE;
    }

    public EnchantmentRestrictionsSection enchantmentRestrictions = new EnchantmentRestrictionsSection();

    public static class EnchantmentRestrictionsSection extends ConfigSection {
        public ValidatedBoolean enabled = new ValidatedBoolean(true);
    }

    public DurabilityReworkSection durabilityRework = new DurabilityReworkSection();

    public static class DurabilityReworkSection extends ConfigSection {
        public ValidatedBoolean enabled = new ValidatedBoolean(true);
    }

    public StarterKitSection starterKit = new StarterKitSection();

    public static class StarterKitSection extends ConfigSection {
        public ValidatedList<String> items = new ValidatedString().toList(
                "ftbquests:book|1|",
                "minecraft:torch|4|",
                "legendarysurvivaloverhaul:bandage|6|"
        );

        public ValidatedList<String> classEntries = new ValidatedString().toList(
                "otherworldorigins:class/artificer|minecraft:iron_pickaxe|1|",
                "otherworldorigins:class/barbarian|minecraft:stone_axe|1|",
                "otherworldorigins:class/bard|immersive_melodies:lute|1|",
                "otherworldorigins:class/cleric|minecraft:book|1|",
                "otherworldorigins:class/druid|minecraft:wooden_hoe|1|",
                "otherworldorigins:class/fighter|minecraft:iron_sword|1|",
                "otherworldorigins:class/monk|minecraft:stick|1|",
                "otherworldorigins:class/paladin|minecraft:shield|1|",
                "otherworldorigins:class/ranger|minecraft:bow|1|",
                "otherworldorigins:class/rogue|minecraft:crossbow|1|",
                "otherworldorigins:class/sorcerer|minecraft:blaze_powder|1|",
                "otherworldorigins:class/warlock|minecraft:ender_pearl|1|",
                "otherworldorigins:class/wizard|minecraft:writable_book|1|"
        );
    }

    public ShoulderSurfingSection shoulderSurfing = new ShoulderSurfingSection();

    public static class ShoulderSurfingSection extends ConfigSection {
        public ValidatedList<String> rotationBlacklist = new ValidatedString().toList(
                "otherworldorigins:dark_vision_toggle",
                "otherworldorigins:cantrips/fortify",
                "otherworldorigins:cantrips/two/fortify",
                "otherworldorigins:cantrips/oakskin",
                "otherworldorigins:cantrips/two/oakskin",
                "otherworldorigins:cantrips/healing_circle",
                "otherworldorigins:cantrips/two/healing_circle",
                "otherworldorigins:cantrips/magical_secrets/fortify",
                "otherworldorigins:cantrips/magical_secrets/oakskin",
                "otherworldorigins:cantrips/magical_secrets/healing_circle",
                "otherworldorigins:class/druid/wildshape_enter_wildshape"
        );
    }

    public static boolean enableEnchantmentRestrictions() {
        return INSTANCE != null && INSTANCE.enchantmentRestrictions.enabled.get();
    }

    public static boolean enableDurabilityRework() {
        return INSTANCE != null && INSTANCE.durabilityRework.enabled.get();
    }

    public static List<String> starterKitItems() {
        return INSTANCE == null ? List.of() : INSTANCE.starterKit.items;
    }

    public static List<String> classStarterKitEntries() {
        return INSTANCE == null ? List.of() : INSTANCE.starterKit.classEntries;
    }

    public static boolean isPowerRotationBlacklisted(String powerId) {
        if (INSTANCE == null) return false;
        for (String pattern : INSTANCE.shoulderSurfing.rotationBlacklist) {
            if (matchesWildcard(powerId, pattern)) {
                return true;
            }
        }
        return false;
    }

    public static boolean areAllPowersRotationBlacklisted(Set<ResourceLocation> powerIds) {
        if (INSTANCE == null) return false;
        if (powerIds.isEmpty()) return true;
        for (ResourceLocation powerId : powerIds) {
            if (!isPowerRotationBlacklisted(powerId.toString())) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesWildcard(String text, String pattern) {
        String regex = pattern
                .replace(".", "\\.")
                .replace("*", ".*");
        try {
            return text.matches(regex);
        } catch (Exception e) {
            return false;
        }
    }
}
