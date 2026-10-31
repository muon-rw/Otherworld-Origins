package dev.muon.raven_dnd_origins.config;

import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.config.ConfigSection;
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedList;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
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

        public ValidatedList<String> originEntries = new ValidatedString().toList(
                "raven_dnd_origins:subclass/artificer/alchemist|raven_core:potion_of_healing|3|",
                "raven_dnd_origins:subclass/artificer/alchemist|minecraft:brewing_stand|1|",
                "raven_dnd_origins:subclass/artificer/armorer|minecraft:iron_chestplate|1|",
                "raven_dnd_origins:subclass/artificer/artillerist|minecraft:crossbow|1|",
                "raven_dnd_origins:subclass/artificer/artillerist|minecraft:arrow|16|",
                "raven_dnd_origins:subclass/artificer/battle_smith|betterend:iron_hammer|1|",
                "raven_dnd_origins:class/barbarian|berserker_rpg:stone_berserker_axe|1|",
                "raven_dnd_origins:subclass/barbarian/wildheart|raven_core:potion_of_mana|3|",
                "raven_dnd_origins:class/bard|immersive_melodies:lute|1|",
                "raven_dnd_origins:subclass/bard/lore|raven_core:potion_of_mana|3|",
                "raven_dnd_origins:subclass/bard/valor|minecraft:shield|1|",
                "raven_dnd_origins:subclass/bard/swords|valoria:iron_rapier|1|",
                "raven_dnd_origins:class/cleric|minecraft:shield|1|",
                "raven_dnd_origins:subclass/cleric/life|raven_core:potion_of_healing|3|",
                "raven_dnd_origins:subclass/cleric/trickery|minecraft:potion|1|{\"minecraft:potion_contents\":{potion:\"minecraft:invisibility\"}}",
                "raven_dnd_origins:subclass/cleric/knowledge|minecraft:lapis_lazuli|16|",
                "raven_dnd_origins:subclass/cleric/tempest|raven_core:potion_of_mana|3|",
                "raven_dnd_origins:subclass/cleric/war|betterend:iron_hammer|1|",
                "raven_dnd_origins:class/druid|rogues:iron_sickle|1|",
                "raven_dnd_origins:class/druid|raven_core:potion_of_mana|3|",
                "raven_dnd_origins:class/fighter|minecraft:iron_sword|1|",
                "raven_dnd_origins:subclass/fighter/battle_master|minecraft:shield|1|",
                "raven_dnd_origins:subclass/fighter/champion|raven_core:potion_of_healing|3|",
                "raven_dnd_origins:subclass/fighter/eldritch_knight|raven_core:potion_of_mana|3|",
                "raven_dnd_origins:class/monk|forcemaster_rpg:stone_knuckle|1|",
                "raven_dnd_origins:subclass/monk/open_hand|raven_core:potion_of_healing|3|",
                "raven_dnd_origins:subclass/monk/four_elements|raven_core:potion_of_mana|3|",
                "raven_dnd_origins:subclass/monk/shadow|minecraft:potion|1|{\"minecraft:potion_contents\":{potion:\"minecraft:night_vision\"}}",
                "raven_dnd_origins:subclass/paladin/ancients|archers:iron_spear|1|",
                "raven_dnd_origins:subclass/paladin/ancients|raven_core:potion_of_healing|3|",
                "raven_dnd_origins:subclass/paladin/devotion|minecraft:iron_sword|1|",
                "raven_dnd_origins:subclass/paladin/devotion|minecraft:shield|1|",
                "raven_dnd_origins:subclass/paladin/vengeance|rogues:iron_double_axe|1|",
                "raven_dnd_origins:subclass/paladin/breaker|minecraft:iron_axe|1|",
                "raven_dnd_origins:subclass/paladin/breaker|minecraft:shield|1|",
                "raven_dnd_origins:class/ranger|minecraft:bow|1|",
                "raven_dnd_origins:class/ranger|minecraft:arrow|16|",
                "raven_dnd_origins:class/rogue|rogues:iron_dagger|1|",
                "raven_dnd_origins:subclass/rogue/thief|minecraft:potion|1|{\"minecraft:potion_contents\":{potion:\"minecraft:invisibility\"}}",
                "raven_dnd_origins:subclass/rogue/arcane_trickster|raven_core:potion_of_mana|3|",
                "raven_dnd_origins:subclass/rogue/assassin|rogues:flint_dagger|1|",
                "raven_dnd_origins:class/sorcerer|irons_spellbooks:copper_spell_book|1|",
                "raven_dnd_origins:class/sorcerer|raven_core:potion_of_mana|3|",
                "raven_dnd_origins:class/warlock|raven_core:potion_of_mana|3|",
                "raven_dnd_origins:subclass/warlock/fiend|ess_requiem:rotten_sickle|1|",
                "raven_dnd_origins:subclass/warlock/great_old_one|irons_spellbooks:copper_spell_book|1|",
                "raven_dnd_origins:class/wizard|irons_spellbooks:copper_spell_book|1|",
                "raven_dnd_origins:subclass/wizard/abjuration|raven_core:potion_of_mana|3|",
                "raven_dnd_origins:subclass/wizard/evocation|raven_core:potion_of_mana|3|",
                "raven_dnd_origins:subclass/wizard/necromancy|raven_core:potion_of_mana|3|",
                "raven_dnd_origins:subclass/wizard/conjuration|raven_core:potion_of_mana|3|",
                "raven_dnd_origins:subclass/wizard/enchanting|minecraft:lapis_lazuli|16|",
                "raven_dnd_origins:subclass/wizard/transmutation|raven_core:potion_of_mana|3|"
        );
    }

    public WeaponSchoolsSection weaponSchools = new WeaponSchoolsSection();

    public static class WeaponSchoolsSection extends ConfigSection {
        public ValidatedDouble weaponWeight = new ValidatedDouble(0.5, 1.0, 0.0);
        public ValidatedDouble martialReferenceDamage = new ValidatedDouble(6.0, 100.0, 1.0);
        public ValidatedDouble archeryReferenceDamage = new ValidatedDouble(6.0, 100.0, 1.0);
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

    public static double weaponSchoolWeight() {
        return INSTANCE == null ? 0.5 : INSTANCE.weaponSchools.weaponWeight.get();
    }

    public static double martialReferenceDamage() {
        return INSTANCE == null ? 6.0 : INSTANCE.weaponSchools.martialReferenceDamage.get();
    }

    public static double archeryReferenceDamage() {
        return INSTANCE == null ? 6.0 : INSTANCE.weaponSchools.archeryReferenceDamage.get();
    }

    public static List<String> starterKitItems() {
        return INSTANCE == null ? List.of() : INSTANCE.starterKit.items;
    }

    public static List<String> originStarterKitEntries() {
        return INSTANCE == null ? List.of() : INSTANCE.starterKit.originEntries;
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
