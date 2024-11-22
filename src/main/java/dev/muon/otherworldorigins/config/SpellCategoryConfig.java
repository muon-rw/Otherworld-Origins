package dev.muon.otherworldorigins.config;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SpellCategoryConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> OFFENSIVE_SPELLS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SUPPORT_SPELLS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> DEFENSIVE_SPELLS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> UNRESTRICTED_SPELLS;
    private static final Map<String, List<String>> DEFAULT_CLASS_RESTRICTIONS = createDefaultRestrictions();

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
            "otherworldorigins:white_dragon_breath"
    );

    private static Map<String, List<String>> createDefaultRestrictions() {
        Map<String, List<String>> restrictions = new TreeMap<>(); // Uses natural (alphabetical) ordering

        // Artificer
        restrictions.put("artificer/alchemist", Arrays.asList("DEFENSIVE"));
        restrictions.put("artificer/armorer", Arrays.asList("DEFENSIVE"));
        restrictions.put("artificer/artillerist", Arrays.asList("OFFENSIVE"));
        restrictions.put("artificer/battle_smith", Arrays.asList("OFFENSIVE"));

        // Barbarian
        restrictions.put("barbarian/berserker", Arrays.asList());
        restrictions.put("barbarian/wild_magic", Arrays.asList());
        restrictions.put("barbarian/wildheart", Arrays.asList());

        // Bard
        restrictions.put("bard/lore", Arrays.asList("OFFENSIVE", "SUPPORT", "DEFENSIVE"));
        restrictions.put("bard/swords", Arrays.asList("OFFENSIVE", "SUPPORT", "DEFENSIVE"));
        restrictions.put("bard/valor", Arrays.asList("OFFENSIVE", "SUPPORT", "DEFENSIVE"));

        // Cleric
        restrictions.put("cleric/knowledge", Arrays.asList("OFFENSIVE", "SUPPORT", "DEFENSIVE"));
        restrictions.put("cleric/life", Arrays.asList("DEFENSIVE", "SUPPORT"));
        restrictions.put("cleric/tempest", Arrays.asList("OFFENSIVE"));
        restrictions.put("cleric/trickery", Arrays.asList("OFFENSIVE"));
        restrictions.put("cleric/war", Arrays.asList("OFFENSIVE", "DEFENSIVE"));

        // Druid
        restrictions.put("druid/land", Arrays.asList("DEFENSIVE", "SUPPORT"));
        restrictions.put("druid/moon", Arrays.asList("OFFENSIVE", "DEFENSIVE"));
        restrictions.put("druid/spores", Arrays.asList("OFFENSIVE"));

        // Fighter
        restrictions.put("fighter/battle_master", Arrays.asList());
        restrictions.put("fighter/champion", Arrays.asList());
        restrictions.put("fighter/eldritch_knight", Arrays.asList("OFFENSIVE", "SUPPORT", "DEFENSIVE"));

        // Monk
        restrictions.put("monk/four_elements", Arrays.asList("OFFENSIVE", "SUPPORT", "DEFENSIVE"));
        restrictions.put("monk/open_hand", Arrays.asList("SUPPORT"));
        restrictions.put("monk/shadow", Arrays.asList());

        // Paladin
        restrictions.put("paladin/ancients", Arrays.asList("DEFENSIVE", "SUPPORT"));
        restrictions.put("paladin/breaker", Arrays.asList("OFFENSIVE", "SUPPORT", "DEFENSIVE"));
        restrictions.put("paladin/devotion", Arrays.asList("OFFENSIVE", "SUPPORT"));
        restrictions.put("paladin/vengeance", Arrays.asList("OFFENSIVE", "DEFENSIVE"));

        // Ranger
        restrictions.put("ranger/beast_master", Arrays.asList("SUPPORT"));
        restrictions.put("ranger/gloom_stalker", Arrays.asList("DEFENSIVE"));
        restrictions.put("ranger/hunter", Arrays.asList("OFFENSIVE"));

        // Rogue
        restrictions.put("rogue/arcane_trickster", Arrays.asList("OFFENSIVE", "SUPPORT", "DEFENSIVE"));
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

    private static final List<String> DEFAULT_OFFENSIVE_SPELLS = Arrays.asList(
            "acupuncture", "blood_needles", "blood_slash", "devour", "heartstop",
            "ray_of_siphoning", "wither_skull", "dragon_breath", "magic_arrow", "magic_missile",
            "starfall", "black_hole", "chain_creeper", "fang_strike", "fang_ward",
            "firecracker", "arrow_volley", "blaze_storm", "fireball", "firebolt",
            "fire_breath", "magma_bomb", "flaming_strike", "scorch", "guiding_bolt",
            "divine_smite", "cone_of_cold", "icicle", "ray_of_frost", "frostwave",
            "chain_lightning", "electrocute", "lightning_bolt", "lightning_lance",
            "shockwave", "thunderstorm", "acid_orb", "blight", "poison_arrow",
            "poison_breath", "poison_splash", "earthquake", "stomp", "sculk_tentacles",
            "sonic_boom", "eldritch_blast", "wisp", "lob_creeper", "burning_dash",
            "wall_of_fire", "ice_block", "summon_vex", "summon_polar_bear", "raise_dead",
            "spectral_hammer"
    );

    private static final List<String> DEFAULT_SUPPORT_SPELLS = Arrays.asList(
            "blessing_of_life", "cloud_of_regeneration", "greater_heal",
            "healing_circle", "heal", "gluttony", "angel_wings", "spectral_hammer",
            "slow", "heat_surge",
            "raise_dead", "portal", "gust", "firefly_swarm", "wololo", "charge", "traveloptics:em_pulse", "blight",
            "acid_spit", "haste", "slow", "gust"
    );

    private static final List<String> DEFAULT_DEFENSIVE_SPELLS = Arrays.asList(
            "counterspell", "evasion", "teleport", "gust", "invisibility", "shield",
            "fortify", "haste", "frost_step", "ascension", "charge", "root",
            "spider_aspect", "firefly_swarm", "oakskin", "abyssal_shroud",
            "planar_sight", "telekinesis", "blood_step", "portal", "slow",
            "wall_of_fire", "ice_block", "spectral_hammer", "angel_wing"
    );

    static {
        BUILDER.push("Spell Configuration");
        BUILDER.comment(
                " Valid spell categories are: OFFENSIVE, SUPPORT, DEFENSIVE",
                " Any spells not listed in any category will default to OFFENSIVE",
                " Spells can be assigned to multiple categories",
                " Spells from Iron's Spellbooks can omit the 'irons_spellbooks:' namespace prefix"
        );

        BUILDER.push("Spell Categories");
        OFFENSIVE_SPELLS = BUILDER
                .comment(" List of spells that deal damage or have harmful effects")
                .defineList("offensive_spells", DEFAULT_OFFENSIVE_SPELLS, SpellCategoryConfig::isValidSpellId);
        SUPPORT_SPELLS = BUILDER
                .comment(" List of spells that heal, buff, or otherwise aid allies")
                .defineList("support_spells", DEFAULT_SUPPORT_SPELLS, SpellCategoryConfig::isValidSpellId);
        DEFENSIVE_SPELLS = BUILDER
                .comment(" List of spells that provide protection, mobility, or control effects")
                .defineList("defensive_spells", DEFAULT_DEFENSIVE_SPELLS, SpellCategoryConfig::isValidSpellId);
        UNRESTRICTED_SPELLS = BUILDER
                .comment(" List of spells that bypass all casting restrictions",
                        " The default list contains Origin spells, but spells will always be",
                        " castable if they are innate Origin abilities, even if they are not on this list.",
                        " These are just here for an example.")
                .defineList("unrestricted_spells", DEFAULT_UNRESTRICTED_SPELLS, SpellCategoryConfig::isValidSpellId);
        BUILDER.pop();

        BUILDER.push("Allowed Spells");
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
        BUILDER.pop();
        SPEC = BUILDER.build();
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
}