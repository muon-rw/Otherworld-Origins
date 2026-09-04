package dev.muon.raven_dnd_origins.config;

import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.config.ConfigSection;
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedList;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;

public class RavenDndOriginsConfig extends Config {
    private static RavenDndOriginsConfig INSTANCE;

    public RavenDndOriginsConfig() {
        super(ResourceLocation.fromNamespaceAndPath("raven_dnd_origins", "config"));
    }

    public static void setInstance(RavenDndOriginsConfig instance) {
        INSTANCE = instance;
    }

    public static RavenDndOriginsConfig getInstance() {
        return INSTANCE;
    }

    public EnchantmentRestrictionsSection enchantmentRestrictions = new EnchantmentRestrictionsSection();

    public static class EnchantmentRestrictionsSection extends ConfigSection {
        public ValidatedBoolean enabled = new ValidatedBoolean(true);
    }

    public SpellRestrictionsSection spellRestrictions = new SpellRestrictionsSection();

    public static class SpellRestrictionsSection extends ConfigSection {
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
                "raven_dnd_origins:class/artificer|minecraft:iron_pickaxe|1|",
                "raven_dnd_origins:class/barbarian|minecraft:stone_axe|1|",
                "raven_dnd_origins:class/bard|immersive_melodies:lute|1|",
                "raven_dnd_origins:class/cleric|minecraft:book|1|",
                "raven_dnd_origins:class/druid|minecraft:wooden_hoe|1|",
                "raven_dnd_origins:class/fighter|minecraft:iron_sword|1|",
                "raven_dnd_origins:class/monk|minecraft:stick|1|",
                "raven_dnd_origins:class/paladin|minecraft:shield|1|",
                "raven_dnd_origins:class/ranger|minecraft:bow|1|",
                "raven_dnd_origins:class/rogue|minecraft:crossbow|1|",
                "raven_dnd_origins:class/sorcerer|minecraft:blaze_powder|1|",
                "raven_dnd_origins:class/warlock|minecraft:ender_pearl|1|",
                "raven_dnd_origins:class/wizard|minecraft:writable_book|1|"
        );
    }

    public ShoulderSurfingSection shoulderSurfing = new ShoulderSurfingSection();

    public static class ShoulderSurfingSection extends ConfigSection {
        public ValidatedList<String> rotationBlacklist = new ValidatedString().toList(
                "raven_dnd_origins:dark_vision_toggle",
                "raven_dnd_origins:cantrips/fortify",
                "raven_dnd_origins:cantrips/two/fortify",
                "raven_dnd_origins:cantrips/oakskin",
                "raven_dnd_origins:cantrips/two/oakskin",
                "raven_dnd_origins:cantrips/healing_circle",
                "raven_dnd_origins:cantrips/two/healing_circle",
                "raven_dnd_origins:cantrips/magical_secrets/fortify",
                "raven_dnd_origins:cantrips/magical_secrets/oakskin",
                "raven_dnd_origins:cantrips/magical_secrets/healing_circle",
                "raven_dnd_origins:class/druid/wildshape_enter_wildshape"
        );
    }

    public static boolean enableEnchantmentRestrictions() {
        return INSTANCE != null && INSTANCE.enchantmentRestrictions.enabled.get();
    }

    public static boolean enableSpellRestrictions() {
        return INSTANCE != null && INSTANCE.spellRestrictions.enabled.get();
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
        // Widen to java.util.List first: FzzyConfig's ValidatedList is a Kotlin type whose supertype
        // closure javac cannot resolve without kotlin-stdlib on the compile classpath.
        List<String> patterns = INSTANCE.shoulderSurfing.rotationBlacklist;
        for (String pattern : patterns) {
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
