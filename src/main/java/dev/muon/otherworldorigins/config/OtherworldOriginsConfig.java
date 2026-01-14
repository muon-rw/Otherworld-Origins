package dev.muon.otherworldorigins.config;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.*;

public class OtherworldOriginsConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> OFFENSIVE_SPELLS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SUPPORT_SPELLS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> DEFENSIVE_SPELLS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> UNRESTRICTED_SPELLS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_ENCHANTMENT_RESTRICTIONS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> STARTER_KIT_ITEMS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SHOULDER_SURFING_ROTATION_BLACKLIST;
    private static final Map<String, List<String>> DEFAULT_CLASS_RESTRICTIONS = createDefaultRestrictions();
    private static final Map<String, List<String>> DEFAULT_SCHOOL_RESTRICTIONS = createDefaultSchoolRestrictions();

    private static final List<String> DEFAULT_UNRESTRICTED_SPELLS = Arrays.asList(
            "otherworldorigins:summon_golem",
            "otherworldorigins:summon_grizzly_bear",
            "otherworldorigins:black_dragon_breath",
            "otherworldorigins:blue_dragon_breath",
            "otherworldorigins:brass_dragon_breath",
            "otherworldorigins:bronze_dragon_breath",
            "otherworldorigins:copper_dragon_breath",
            "otherworldorigins:gold_dragon_breath",
            "otherworldorigins:red_dragon_breath",
            "otherworldorigins:silver_dragon_breath",
            "otherworldorigins:white_dragon_breath",
            "counterspell"
    );

    private static Map<String, List<String>> createDefaultRestrictions() {
        Map<String, List<String>> restrictions = new TreeMap<>(); // Uses natural (alphabetical) ordering

        // Artificer
        restrictions.put("artificer/alchemist", Arrays.asList("DEFENSIVE", "SUPPORT"));
        restrictions.put("artificer/armorer", Arrays.asList("DEFENSIVE"));
        restrictions.put("artificer/artillerist", Arrays.asList("OFFENSIVE"));
        restrictions.put("artificer/battle_smith", Arrays.asList("OFFENSIVE", "DEFENSIVE", "SUPPORT"));

        // Barbarian
        restrictions.put("barbarian/berserker", Arrays.asList());
        restrictions.put("barbarian/wild_magic", Arrays.asList());
        restrictions.put("barbarian/wildheart", Arrays.asList());

        // Bard
        restrictions.put("bard/lore", Arrays.asList("OFFENSIVE", "SUPPORT", "DEFENSIVE"));
        restrictions.put("bard/swords", Arrays.asList("OFFENSIVE", "SUPPORT", "DEFENSIVE"));
        restrictions.put("bard/valor", Arrays.asList("OFFENSIVE", "SUPPORT", "DEFENSIVE"));

        // Cleric
        restrictions.put("cleric/knowledge", Arrays.asList("SUPPORT", "DEFENSIVE"));
        restrictions.put("cleric/life", Arrays.asList("DEFENSIVE", "SUPPORT"));
        restrictions.put("cleric/tempest", Arrays.asList("OFFENSIVE"));
        restrictions.put("cleric/trickery", Arrays.asList("SUPPORT", "DEFENSIVE"));
        restrictions.put("cleric/war", Arrays.asList("OFFENSIVE", "DEFENSIVE"));

        // Druid
        restrictions.put("druid/land", Arrays.asList("DEFENSIVE", "SUPPORT"));
        restrictions.put("druid/moon", Arrays.asList("OFFENSIVE", "DEFENSIVE"));
        restrictions.put("druid/spores", Arrays.asList("OFFENSIVE"));

        // Fighter
        restrictions.put("fighter/battle_master", Arrays.asList());
        restrictions.put("fighter/champion", Arrays.asList());
        restrictions.put("fighter/eldritch_knight", Arrays.asList("OFFENSIVE"));

        // Monk
        restrictions.put("monk/four_elements", Arrays.asList());
        restrictions.put("monk/open_hand", Arrays.asList("SUPPORT"));
        restrictions.put("monk/shadow", Arrays.asList("DEFENSIVE"));

        // Paladin
        restrictions.put("paladin/ancients", Arrays.asList("DEFENSIVE", "SUPPORT"));
        restrictions.put("paladin/breaker", Arrays.asList("OFFENSIVE", "DEFENSIVE"));
        restrictions.put("paladin/devotion", Arrays.asList("OFFENSIVE", "SUPPORT"));
        restrictions.put("paladin/vengeance", Arrays.asList("OFFENSIVE", "DEFENSIVE"));

        // Ranger
        restrictions.put("ranger/beast_master", Arrays.asList("SUPPORT"));
        restrictions.put("ranger/gloom_stalker", Arrays.asList("DEFENSIVE"));
        restrictions.put("ranger/hunter", Arrays.asList("OFFENSIVE"));

        // Rogue
        restrictions.put("rogue/arcane_trickster", Arrays.asList("SUPPORT", "DEFENSIVE"));
        restrictions.put("rogue/assassin", Arrays.asList("OFFENSIVE"));
        restrictions.put("rogue/thief", Arrays.asList("DEFENSIVE"));

        // Sorcerer
        restrictions.put("sorcerer/draconic_bloodline", Arrays.asList("OFFENSIVE", "SUPPORT", "DEFENSIVE"));
        restrictions.put("sorcerer/wild_magic", Arrays.asList("OFFENSIVE", "SUPPORT", "DEFENSIVE"));

        // Warlock
        restrictions.put("warlock/fiend", Arrays.asList("OFFENSIVE", "SUPPORT", "DEFENSIVE"));
        restrictions.put("warlock/great_old_one", Arrays.asList("OFFENSIVE", "SUPPORT", "DEFENSIVE"));

        // Wizard
        restrictions.put("wizard/abjuration", Arrays.asList("OFFENSIVE", "SUPPORT", "DEFENSIVE"));
        restrictions.put("wizard/conjuration", Arrays.asList("OFFENSIVE", "SUPPORT", "DEFENSIVE"));
        restrictions.put("wizard/enchanting", Arrays.asList("OFFENSIVE", "SUPPORT", "DEFENSIVE"));
        restrictions.put("wizard/evocation", Arrays.asList("OFFENSIVE", "SUPPORT", "DEFENSIVE"));
        restrictions.put("wizard/necromancy", Arrays.asList("OFFENSIVE", "SUPPORT", "DEFENSIVE"));
        restrictions.put("wizard/transmutation", Arrays.asList("OFFENSIVE", "SUPPORT", "DEFENSIVE"));

        return restrictions;
    }

    private static Map<String, List<String>> createDefaultSchoolRestrictions() {
        Map<String, List<String>> restrictions = new TreeMap<>();

        // Empty defaults - schools will be configured per-subclass
        // Format: "class/subclass" -> list of allowed school IDs (e.g., "irons_spellbooks:holy", "irons_spellbooks:fire")
        // An empty list means no additional schools beyond category restrictions
        // Schools are additive to category restrictions
        
        // Artificer
        restrictions.put("artificer/alchemist", Arrays.asList("irons_spellbooks:nature", "irons_spellbooks:aqua"));
        restrictions.put("artificer/armorer", Arrays.asList("irons_spellbooks:lightning"));
        restrictions.put("artificer/artillerist", Arrays.asList("irons_spellbooks:fire", "irons_spellbooks:lightning"));
        restrictions.put("artificer/battle_smith", Arrays.asList());

        // Barbarian
        restrictions.put("barbarian/berserker", Collections.emptyList());
        restrictions.put("barbarian/wild_magic", Collections.emptyList());
        restrictions.put("barbarian/wildheart", Collections.emptyList());

        // Bard
        restrictions.put("bard/lore", Arrays.asList());
        restrictions.put("bard/swords", Arrays.asList());
        restrictions.put("bard/valor", Arrays.asList());

        // Cleric
        restrictions.put("cleric/knowledge", Arrays.asList("irons_spellbooks:holy", "irons_spellbooks:ender"));
        restrictions.put("cleric/life", Arrays.asList("irons_spellbooks:holy"));
        restrictions.put("cleric/tempest", Arrays.asList("irons_spellbooks:holy", "irons_spellbooks:lightning", "irons_spellbooks:aqua"));
        restrictions.put("cleric/trickery", Arrays.asList("irons_spellbooks:holy", "irons_spellbooks:ender"));
        restrictions.put("cleric/war", Arrays.asList("irons_spellbooks:holy"));

        // Druid
        restrictions.put("druid/land", Arrays.asList("irons_spellbooks:nature", "gtbcs_geomancy_plus:geo", "traveloptics:aqua"));
        restrictions.put("druid/moon", Arrays.asList("irons_spellbooks:nature", "gtbcs_geomancy_plus:geo"));
        restrictions.put("druid/spores", Arrays.asList("irons_spellbooks:nature", "irons_spellbooks:blood"));

        // Fighter
        restrictions.put("fighter/battle_master", Collections.emptyList());
        restrictions.put("fighter/champion", Collections.emptyList());
        restrictions.put("fighter/eldritch_knight", Arrays.asList("irons_spellbooks:fire", "irons_spellbooks:ice", "irons_spellbooks:lightning"));

        // Monk
        restrictions.put("monk/four_elements", Arrays.asList("irons_spellbooks:fire",
                "irons_spellbooks:ice",
                "irons_spellbooks:lightning",
                "irons_spellbooks:nature",
                "traveloptics:aqua",
                "gtbcs_geomancy_plus:geo"));
        restrictions.put("monk/open_hand", Arrays.asList());
        restrictions.put("monk/shadow", Arrays.asList("irons_spellbooks:ender"));

        // Paladin
        restrictions.put("paladin/ancients", Arrays.asList("irons_spellbooks:holy", "irons_spellbooks:nature"));
        restrictions.put("paladin/breaker", Arrays.asList("irons_spellbooks:blood"));
        restrictions.put("paladin/devotion", Arrays.asList("irons_spellbooks:holy"));
        restrictions.put("paladin/vengeance", Arrays.asList("irons_spellbooks:holy"));

        // Ranger
        restrictions.put("ranger/beast_master", Arrays.asList("irons_spellbooks:nature"));
        restrictions.put("ranger/gloom_stalker", Arrays.asList("irons_spellbooks:nature", "irons_spellbooks:ender"));
        restrictions.put("ranger/hunter", Arrays.asList("irons_spellbooks:nature"));

        // Rogue
        restrictions.put("rogue/arcane_trickster", Arrays.asList("irons_spellbooks:ender", "irons_spellbooks:eldritch"));
        restrictions.put("rogue/assassin", Arrays.asList("irons_spellbooks:blood", "irons_spellbooks:nature"));
        restrictions.put("rogue/thief", Arrays.asList("irons_spellbooks:ender"));

        // Sorcerer
        restrictions.put("sorcerer/draconic_bloodline", Collections.emptyList());
        restrictions.put("sorcerer/wild_magic", Collections.emptyList());

        // Warlock
        restrictions.put("warlock/fiend", Collections.emptyList());
        restrictions.put("warlock/great_old_one", Collections.emptyList());

        // Wizard
        restrictions.put("wizard/abjuration", Collections.emptyList());
        restrictions.put("wizard/conjuration", Collections.emptyList());
        restrictions.put("wizard/enchanting", Collections.emptyList());
        restrictions.put("wizard/evocation", Collections.emptyList());
        restrictions.put("wizard/necromancy", Collections.emptyList());
        restrictions.put("wizard/transmutation", Collections.emptyList());

        return restrictions;
    }

    private static final List<String> DEFAULT_OFFENSIVE_SPELLS = Arrays.asList(
            "acupuncture", "blood_needles", "blood_slash", "devour",
            "ray_of_siphoning", "wither_skull", "dragon_breath", "magic_arrow", "magic_missile",
            "starfall", "black_hole", "chain_creeper", "fang_strike", "fang_ward",
            "firecracker", "arrow_volley", "blaze_storm", "fireball", "firebolt",
            "fire_breath", "magma_bomb", "flaming_strike", "scorch", "guiding_bolt",
            "divine_smite", "cone_of_cold", "icicle", "ray_of_frost", "frostwave",
            "chain_lightning", "electrocute", "lightning_bolt", "lightning_lance",
            "shockwave", "thunderstorm", "acid_orb", "blight", "poison_arrow",
            "poison_breath", "poison_splash", "earthquake", "stomp", "sculk_tentacles",
            "sonic_boom", "eldritch_blast", "wisp", "lob_creeper", "burning_dash",
            "wall_of_fire", "ice_block", "summon_vex", "summon_polar_bear", "raise_dead"
    );

    private static final List<String> DEFAULT_SUPPORT_SPELLS = Arrays.asList(
            "blessing_of_life", "cloud_of_regeneration", "greater_heal",
            "healing_circle", "heal", "gluttony", "angel_wings", "spectral_hammer",
            "slow", "heat_surge",
            "raise_dead", "portal", "gust", "firefly_swarm", "wololo", "charge", "traveloptics:em_pulse", "blight",
            "acid_spit", "haste", "slow", "gust", "fortify",
            "spectral_hammer", "alshanex_familiars:rhapsody", "traveloptics:rainfall",
            "gtbcs_geomancy_plus:geo_conductor", "cleanse", "alshanex_familiars:celestial_chant",
            "alshanex_familiars:bird_spell", "acid_orb"
    );

    private static final List<String> DEFAULT_DEFENSIVE_SPELLS = Arrays.asList(
            "counterspell", "evasion", "teleport", "gust", "invisibility", "shield",
            "haste", "frost_step", "ascension", "charge", "root",
            "spider_aspect", "firefly_swarm", "oakskin", "abyssal_shroud",
            "planar_sight", "telekinesis", "blood_step", "portal", "slow",
            "wall_of_fire", "ice_block", "spectral_hammer", "angel_wing",
            "heartstop", "fortify", "healing_circle", "traveloptics:rainfall",
            "traveloptics:jetstream", "traveloptics:flames_reborn", "ice_tomb",
            "cleanse", "gtbcs_geomancy_plus:seismic_surf", "alshanex_familiars:celestial_chant"
    );

    static {
        BUILDER.push("Otherworld Origins Config");
        BUILDER.push("Enchantment Restrictions");
        ENABLE_ENCHANTMENT_RESTRICTIONS = BUILDER
                .comment(" Enable class-based enchantment restrictions")
                .define("enableEnchantmentRestrictions", true);
        BUILDER.pop();
        BUILDER.push("Spell Restrictions");
        BUILDER.comment(
                " Valid spell categories are: OFFENSIVE, SUPPORT, DEFENSIVE",
                " Any spells not listed in any category will default to OFFENSIVE",
                " Spells can be assigned to multiple categories",
                " Spells from Iron's Spellbooks can omit the 'irons_spellbooks:' namespace prefix"
        );
        BUILDER.pop();
        BUILDER.push("Spell Classification");
        OFFENSIVE_SPELLS = BUILDER
                .comment(" List of spells that deal damage or have harmful effects")
                .defineList("offensive_spells", DEFAULT_OFFENSIVE_SPELLS, OtherworldOriginsConfig::isValidSpellId);
        SUPPORT_SPELLS = BUILDER
                .comment(" List of spells that heal, buff, or otherwise aid allies")
                .defineList("support_spells", DEFAULT_SUPPORT_SPELLS, OtherworldOriginsConfig::isValidSpellId);
        DEFENSIVE_SPELLS = BUILDER
                .comment(" List of spells that provide protection, mobility, or control effects")
                .defineList("defensive_spells", DEFAULT_DEFENSIVE_SPELLS, OtherworldOriginsConfig::isValidSpellId);
        UNRESTRICTED_SPELLS = BUILDER
                .comment(" List of spells that bypass all casting restrictions",
                        " The default list contains Origin spells, but this is just an example - ",
                        " Spells will always be castable if they are innate Origin abilities, even if they are not on this list.")
                .defineList("unrestricted_spells", DEFAULT_UNRESTRICTED_SPELLS, OtherworldOriginsConfig::isValidSpellId);
        BUILDER.pop();

        BUILDER.push("Allowed Spells per Class");
        BUILDER.comment(
                " Only edit values inside of the square brackets []",
                " To remove restrictions, add all 3 categories (OFFENSIVE, DEFENSIVE, SUPPORT) to the array for the listed subclass.",
                " An empty array means this subclass can not cast spells.",
                "# Do not delete rows from this list!"
        );
        for (Map.Entry<String, List<String>> entry : DEFAULT_CLASS_RESTRICTIONS.entrySet()) {
            BUILDER.define(entry.getKey(), entry.getValue(), value -> {
                if (!(value instanceof List)) return false;
                return ((List<?>) value).stream()
                        .allMatch(item -> item instanceof String &&
                                Arrays.asList("OFFENSIVE", "SUPPORT", "DEFENSIVE")
                                        .contains(((String) item).toUpperCase()));
            });
        }
        BUILDER.pop();

        BUILDER.push("Allowed Schools per Class");
        BUILDER.comment(
                " Schools are additive to category restrictions - a spell is allowed if it matches either a category OR a school.",
                " School IDs use the format 'namespace:school_name' (e.g., 'irons_spellbooks:holy', 'irons_spellbooks:fire').",
                " Common schools: holy, blood, ender, evocation, fire, ice, lightning, nature, eldritch",
                " An empty array [] means no additional schools are allowed beyond category restrictions.",
                " This setting is ignored for subclasses that already have access to all spell categories."
        );
        for (Map.Entry<String, List<String>> entry : DEFAULT_SCHOOL_RESTRICTIONS.entrySet()) {
            BUILDER.define(entry.getKey(), entry.getValue(), value -> {
                if (!(value instanceof List)) return false;
                return ((List<?>) value).stream()
                        .allMatch(item -> item instanceof String);
            });
        }
        BUILDER.pop();

        BUILDER.push("Starter Kit");
        BUILDER.comment(
                " Items given to players when they confirm their character creation.",
                " Format: \"item_id|count|nbt\" where:",
                "   - item_id: The item resource location (e.g., minecraft:iron_sword)",
                "   - count: The number of items (e.g., 1)",
                "   - nbt: Optional NBT data as JSON string (can be empty, e.g., {} or leave empty)",
                " Example: \"minecraft:iron_sword|1|\" or \"minecraft:enchanted_book|1|{\\\"StoredEnchantments\\\":[{\\\"id\\\":\\\"minecraft:sharpness\\\",\\\"lvl\\\":1}]}\""
        );
        STARTER_KIT_ITEMS = BUILDER
                .comment(" List of starter kit items in format: item_id|count|nbt")
                .defineList("starter_kit_items", Arrays.asList(
                        "ftbquests:book|1|",
                        "minecraft:torch|4|",
                        "legendarysurvivaloverhaul:bandage|6|"
                ), value -> {
                    if (!(value instanceof String str)) return false;
                    String[] parts = str.split("\\|", 3);
                    return parts.length >= 2; // At least item_id and count
                });
        BUILDER.pop();

        BUILDER.push("Shoulder Surfing Integration");
        BUILDER.comment(
                " Powers that should NOT trigger player rotation toward the crosshair when activated.",
                " If ALL powers being activated match the blacklist, rotation is skipped.",
                " Supports wildcards: * matches any characters.",
                " Examples:",
                "   - \"origins:*\" matches all powers from the origins namespace",
                "   - \"*toggle*\" matches any power containing 'toggle'",
                "   - \"otherworldorigins:some_power\" matches exactly that power"
        );
        SHOULDER_SURFING_ROTATION_BLACKLIST = BUILDER
                .comment(" List of power patterns to exclude from shoulder surfing rotation")
                .defineList("rotation_blacklist", Arrays.asList(
                        "otherworldorigins:dark_vision_toggle",
                        "otherworldorigins:cantrips/fortify",
                        "otherworldorigins:cantrips/two/fortify"
                ), value -> value instanceof String);
        BUILDER.pop();

        SPEC = BUILDER.build();
        BUILDER.pop();
    }

    public static Map<String, List<String>> getClassRestrictions() {
        Map<String, List<String>> restrictions = new TreeMap<>();
        if (!SPEC.isLoaded()) return DEFAULT_CLASS_RESTRICTIONS;

        for (Map.Entry<String, List<String>> entry : DEFAULT_CLASS_RESTRICTIONS.entrySet()) {
            String path = entry.getKey();

            ForgeConfigSpec.ConfigValue<?> configValue = SPEC.getValues().get(Arrays.asList("Spell Configuration", "Allowed Spells", path));

            if (configValue != null) {
                @SuppressWarnings("unchecked")
                List<String> categories = (List<String>) configValue.get();
                restrictions.put(path, categories);
            } else {
                restrictions.put(path, entry.getValue());
            }
        }

        return restrictions;
    }

    public static Map<String, List<String>> getSchoolRestrictions() {
        Map<String, List<String>> restrictions = new TreeMap<>();
        if (!SPEC.isLoaded()) return DEFAULT_SCHOOL_RESTRICTIONS;

        for (Map.Entry<String, List<String>> entry : DEFAULT_SCHOOL_RESTRICTIONS.entrySet()) {
            String path = entry.getKey();

            ForgeConfigSpec.ConfigValue<?> configValue = SPEC.getValues().get(Arrays.asList("Spell Configuration", "Allowed Schools", path));

            if (configValue != null) {
                @SuppressWarnings("unchecked")
                List<String> schools = (List<String>) configValue.get();
                restrictions.put(path, schools);
            } else {
                restrictions.put(path, entry.getValue());
            }
        }

        return restrictions;
    }

    private static boolean isValidSpellId(Object obj) {
        if (!(obj instanceof String str)) {
            return false;
        }
        try {
            ResourceLocation loc;
            if (str.contains(":")) {
                loc = new ResourceLocation(str);
            } else {
                loc = new ResourceLocation("irons_spellbooks", str);
            }
            return SpellRegistry.getSpell(loc) != SpellRegistry.none();
        } catch (Exception e) {
            return false;
        }
    }


    public static boolean isSpellUnrestricted(AbstractSpell spell) {
        if (!SPEC.isLoaded()) return false;

        String spellId = spell.getSpellId();
        String spellName = spell.getSpellName();

        List<? extends String> unrestrictedSpells = UNRESTRICTED_SPELLS.get();

        return unrestrictedSpells.contains(spellId) || unrestrictedSpells.contains(spellName);
    }

    /**
     * Checks if a power ID matches any pattern in the shoulder surfing rotation blacklist.
     * @param powerId The power resource location string (e.g., "otherworldorigins:some_power")
     * @return true if the power matches a blacklist pattern
     */
    public static boolean isPowerRotationBlacklisted(String powerId) {
        if (!SPEC.isLoaded()) return false;

        List<? extends String> blacklist = SHOULDER_SURFING_ROTATION_BLACKLIST.get();
        for (String pattern : blacklist) {
            if (matchesWildcard(powerId, pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if all powers in a set are blacklisted from rotation.
     * @param powerIds Set of power resource location strings
     * @return true if ALL powers match blacklist patterns (rotation should be skipped)
     */
    public static boolean areAllPowersRotationBlacklisted(java.util.Set<ResourceLocation> powerIds) {
        if (!SPEC.isLoaded()) return false;
        if (powerIds.isEmpty()) return true;

        for (ResourceLocation powerId : powerIds) {
            if (!isPowerRotationBlacklisted(powerId.toString())) {
                return false; // At least one power is not blacklisted
            }
        }
        return true;
    }

    /**
     * Simple wildcard matching where * matches any sequence of characters.
     */
    private static boolean matchesWildcard(String text, String pattern) {
        // Convert wildcard pattern to regex
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