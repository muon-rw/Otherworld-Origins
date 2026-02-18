package dev.muon.otherworldorigins.item;

import dev.muon.otherworldorigins.OtherworldOrigins;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, OtherworldOrigins.MODID);


    /** Races + Subraces */
    public static final RegistryObject<Item> BASE_PORTRAIT = ITEMS.register("portrait/base", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BLACK_PORTRAIT = ITEMS.register("portrait/black", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BLUE_PORTRAIT = ITEMS.register("portrait/blue", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BRASS_PORTRAIT = ITEMS.register("portrait/brass", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BRONZE_PORTRAIT = ITEMS.register("portrait/bronze", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> COPPER_PORTRAIT = ITEMS.register("portrait/copper", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DEEP_PORTRAIT = ITEMS.register("portrait/deep", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DRAGONBORN_PORTRAIT = ITEMS.register("portrait/dragonborn", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DROW_PORTRAIT = ITEMS.register("portrait/drow", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DUERGAR_PORTRAIT = ITEMS.register("portrait/duergar", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DWARF_PORTRAIT = ITEMS.register("portrait/dwarf", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ELF_PORTRAIT = ITEMS.register("portrait/elf", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ENDERMAN_PORTRAIT = ITEMS.register("portrait/enderman", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> FOREST_PORTRAIT = ITEMS.register("portrait/forest", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GOBLIN_PORTRAIT = ITEMS.register("portrait/goblin", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> HOBGOBLIN_PORTRAIT = ITEMS.register("portrait/hobgoblin", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GITHYANKI_PORTRAIT = ITEMS.register("portrait/githyanki", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GNOME_PORTRAIT = ITEMS.register("portrait/gnome", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GOLD_PORTRAIT = ITEMS.register("portrait/gold", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> HALF_ELF_PORTRAIT = ITEMS.register("portrait/half_elf", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> HALFLING_PORTRAIT = ITEMS.register("portrait/halfling", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> HIGH_PORTRAIT = ITEMS.register("portrait/high", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> HILL_PORTRAIT = ITEMS.register("portrait/hill", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> HUMAN_PORTRAIT = ITEMS.register("portrait/human", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> LIGHTFOOT_PORTRAIT = ITEMS.register("portrait/lightfoot", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> OTHER_PORTRAIT = ITEMS.register("portrait/other", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MOUNTAIN_PORTRAIT = ITEMS.register("portrait/mountain", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PIGLIN_PORTRAIT = ITEMS.register("portrait/piglin", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PILLAGER_PORTRAIT = ITEMS.register("portrait/pillager", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> RED_PORTRAIT = ITEMS.register("portrait/red", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ROCK_PORTRAIT = ITEMS.register("portrait/rock", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SILVER_PORTRAIT = ITEMS.register("portrait/silver", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STOUT_PORTRAIT = ITEMS.register("portrait/stout", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> TIEFLING_PORTRAIT = ITEMS.register("portrait/tiefling", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> UNDEAD_PORTRAIT = ITEMS.register("portrait/undead", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> VARIANT_PORTRAIT = ITEMS.register("portrait/variant", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WHITE_PORTRAIT = ITEMS.register("portrait/white", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WOOD_PORTRAIT = ITEMS.register("portrait/wood", () -> new Item(new Item.Properties()));


    /** Classes */
    public static final RegistryObject<Item> ARTIFICER = ITEMS.register("class/artificer", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BARBARIAN = ITEMS.register("class/barbarian", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BARD = ITEMS.register("class/bard", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> FIGHTER = ITEMS.register("class/fighter", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> RANGER = ITEMS.register("class/ranger", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ROGUE = ITEMS.register("class/rogue", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SORCERER = ITEMS.register("class/sorcerer", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WARLOCK = ITEMS.register("class/warlock", () -> new Item(new Item.Properties()));


    /** Subclasses */
    public static final RegistryObject<Item> ALCHEMIST = ITEMS.register("subclass/artificer/alchemist", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BATTLE_SMITH = ITEMS.register("subclass/artificer/battle_smith", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> VALOR = ITEMS.register("subclass/bard/valor", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> LIFE = ITEMS.register("subclass/cleric/life", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> TEMPEST = ITEMS.register("subclass/cleric/tempest", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> TRICKERY = ITEMS.register("subclass/cleric/trickery", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WAR = ITEMS.register("subclass/cleric/war", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> MOON = ITEMS.register("subclass/druid/moon", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SPORES = ITEMS.register("subclass/druid/spores", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> BATTLE_MASTER = ITEMS.register("subclass/fighter/battle_master", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CHAMPION = ITEMS.register("subclass/fighter/champion", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> FOUR_ELEMENTS = ITEMS.register("subclass/monk/four_elements", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ANCIENTS = ITEMS.register("subclass/paladin/ancients", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DEVOTION = ITEMS.register("subclass/paladin/devotion", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> VENGEANCE = ITEMS.register("subclass/paladin/vengeance", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> GLOOM_STALKER = ITEMS.register("subclass/ranger/gloom_stalker", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> HUNTER = ITEMS.register("subclass/ranger/hunter", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ASSASSIN = ITEMS.register("subclass/rogue/assassin", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> THIEF = ITEMS.register("subclass/rogue/thief", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ARCANE_TRICKSTER = ITEMS.register("subclass/rogue/arcane_trickster", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> DRACONIC_BLOODLINE = ITEMS.register("subclass/sorcerer/draconic_bloodline", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> GREAT_OLD_ONE = ITEMS.register("subclass/warlock/great_old_one", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> FIEND = ITEMS.register("subclass/warlock/fiend", () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> ABJURATION = ITEMS.register("subclass/wizard/abjuration", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CONJURATION = ITEMS.register("subclass/wizard/conjuration", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> EVOCATION = ITEMS.register("subclass/wizard/evocation", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> NECROMANCY = ITEMS.register("subclass/wizard/necromancy", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> TRANSMUTATION = ITEMS.register("subclass/wizard/transmutation", () -> new Item(new Item.Properties()));

    /** Feats */
    public static final RegistryObject<Item> CHEF = ITEMS.register("feats/chef", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DUAL_WIELDER = ITEMS.register("feats/dual_wielder", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> LUCKY = ITEMS.register("feats/lucky", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MAGE_SLAYER = ITEMS.register("feats/mage_slayer", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> POLEARM_MASTER = ITEMS.register("feats/polearm_master", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SHIELD_MASTER = ITEMS.register("feats/shield_master", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> TOUGH = ITEMS.register("feats/tough", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SKILLED = ITEMS.register("feats/skilled", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MOBILE = ITEMS.register("feats/mobile", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DEFENSIVE_DUELIST = ITEMS.register("feats/defensive_duelist", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SAVAGE_ATTACKER = ITEMS.register("feats/savage_attacker", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> RESILIENT = ITEMS.register("feats/resilient", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> TWO_HANDED = ITEMS.register("feats/two_handed", () -> new Item(new Item.Properties()));

    /** Consumables */
    public static final RegistryObject<Item> GOODBERRY = ITEMS.register("goodberry", () -> new GoodberryItem(new Item.Properties()));

    /** Cantrips */
    public static final RegistryObject<Item> GUST = ITEMS.register("cantrips/gust", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ICICLE = ITEMS.register("cantrips/icicle", () -> new Item(new Item.Properties()));

    /** Apts */
    public static final RegistryObject<Item> BLD = ITEMS.register("apts/bld", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STR = ITEMS.register("apts/str", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DEF = ITEMS.register("apts/def", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DEX = ITEMS.register("apts/dex", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> INT = ITEMS.register("apts/int", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CONST = ITEMS.register("apts/const", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> LCK = ITEMS.register("apts/lck", () -> new Item(new Item.Properties()));


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}