package dev.muon.otherworldorigins.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.*;

public class OtherworldOriginsConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLE_ENCHANTMENT_RESTRICTIONS;
    public static final ForgeConfigSpec.BooleanValue ENABLE_DURABILITY_REWORK;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> STARTER_KIT_ITEMS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CLASS_STARTER_KIT_ENTRIES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SHOULDER_SURFING_ROTATION_BLACKLIST;

    static {
        BUILDER.push("Otherworld Origins Config");
        BUILDER.push("Enchantment Restrictions");
        ENABLE_ENCHANTMENT_RESTRICTIONS = BUILDER
                .comment(" Enable class-based enchantment restrictions")
                .define("enableEnchantmentRestrictions", true);
        BUILDER.pop();
        BUILDER.push("Durability Rework");
        ENABLE_DURABILITY_REWORK = BUILDER
                .comment("Enable Durability Rework")
                .define("enableDurabilityRework", true);
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
                    return parts.length >= 2;
                });
        BUILDER.comment(
                " Extra items for players who finished character creation with a specific class origin",
                " on layer otherworldorigins:class (checked on server when they confirm the final screen).",
                " Format: \"class_origin_id|item_id|count|nbt\" where:",
                "   - class_origin_id: Full origin ID on the class layer (e.g. otherworldorigins:class/bard)",
                "   - item_id, count, nbt: Same as starter_kit_items (nbt optional)"
        );
        CLASS_STARTER_KIT_ENTRIES = BUILDER
                .comment(" Per-class starter kit rows: class_origin_id|item_id|count|nbt")
                .defineList("class_starter_kit_entries", Arrays.asList(
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
                ), value -> {
                    if (!(value instanceof String str)) return false;
                    String[] parts = str.split("\\|", 4);
                    if (parts.length < 3) return false;
                    try {
                        ResourceLocation.parse(parts[0].trim());
                        ResourceLocation.parse(parts[1].trim());
                        Integer.parseInt(parts[2].trim());
                    } catch (Exception e) {
                        return false;
                    }
                    return true;
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
                        "otherworldorigins:cantrips/two/fortify",
                        "otherworldorigins:cantrips/oakskin",
                        "otherworldorigins:cantrips/two/oakskin",
                        "otherworldorigins:cantrips/healing_circle",
                        "otherworldorigins:cantrips/two/healing_circle",
                        "otherworldorigins:cantrips/magical_secrets/fortify",
                        "otherworldorigins:cantrips/magical_secrets/oakskin",
                        "otherworldorigins:cantrips/magical_secrets/healing_circle",
                        "otherworldorigins:class/druid/wildshape_enter_wildshape"
                ), value -> value instanceof String);
        BUILDER.pop();

        SPEC = BUILDER.build();
        BUILDER.pop();
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
