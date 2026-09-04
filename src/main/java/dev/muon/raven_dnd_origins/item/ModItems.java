package dev.muon.raven_dnd_origins.item;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.selection.SelectionLayers;
import io.redspace.ironsspellbooks.item.UpgradeOrbItem;
import io.redspace.ironsspellbooks.registries.ComponentRegistry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RavenDndOrigins.MODID);


    /** Races + Subraces */
    public static final DeferredItem<Item> BASE_PORTRAIT = ITEMS.register("portrait/base", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BLACK_PORTRAIT = ITEMS.register("portrait/black", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BLUE_PORTRAIT = ITEMS.register("portrait/blue", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BRASS_PORTRAIT = ITEMS.register("portrait/brass", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BRONZE_PORTRAIT = ITEMS.register("portrait/bronze", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> COPPER_PORTRAIT = ITEMS.register("portrait/copper", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DEEP_PORTRAIT = ITEMS.register("portrait/deep", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DRAGONBORN_PORTRAIT = ITEMS.register("portrait/dragonborn", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DROW_PORTRAIT = ITEMS.register("portrait/drow", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DUERGAR_PORTRAIT = ITEMS.register("portrait/duergar", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DWARF_PORTRAIT = ITEMS.register("portrait/dwarf", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ELF_PORTRAIT = ITEMS.register("portrait/elf", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ENDERMAN_PORTRAIT = ITEMS.register("portrait/enderman", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> FOREST_PORTRAIT = ITEMS.register("portrait/forest", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> GOBLIN_PORTRAIT = ITEMS.register("portrait/goblin", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> HOBGOBLIN_PORTRAIT = ITEMS.register("portrait/hobgoblin", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> GITHYANKI_PORTRAIT = ITEMS.register("portrait/githyanki", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> GNOME_PORTRAIT = ITEMS.register("portrait/gnome", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> GOLD_PORTRAIT = ITEMS.register("portrait/gold", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> HALF_ELF_PORTRAIT = ITEMS.register("portrait/half_elf", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> HALFLING_PORTRAIT = ITEMS.register("portrait/halfling", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> HIGH_PORTRAIT = ITEMS.register("portrait/high", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> HILL_PORTRAIT = ITEMS.register("portrait/hill", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> HUMAN_PORTRAIT = ITEMS.register("portrait/human", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LIGHTFOOT_PORTRAIT = ITEMS.register("portrait/lightfoot", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> OTHER_PORTRAIT = ITEMS.register("portrait/other", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MOUNTAIN_PORTRAIT = ITEMS.register("portrait/mountain", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> PIGLIN_PORTRAIT = ITEMS.register("portrait/piglin", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> PILLAGER_PORTRAIT = ITEMS.register("portrait/pillager", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RED_PORTRAIT = ITEMS.register("portrait/red", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ROCK_PORTRAIT = ITEMS.register("portrait/rock", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SILVER_PORTRAIT = ITEMS.register("portrait/silver", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SKELETON_PORTRAIT = ITEMS.register("portrait/skeleton", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> STOUT_PORTRAIT = ITEMS.register("portrait/stout", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TIEFLING_PORTRAIT = ITEMS.register("portrait/tiefling", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> UNDEAD_PORTRAIT = ITEMS.register("portrait/undead", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> VARIANT_PORTRAIT = ITEMS.register("portrait/variant", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> WHITE_PORTRAIT = ITEMS.register("portrait/white", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> WITHER_PORTRAIT = ITEMS.register("portrait/wither", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> WOOD_PORTRAIT = ITEMS.register("portrait/wood", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ZOMBIE_PORTRAIT = ITEMS.register("portrait/zombie", () -> new Item(new Item.Properties()));


    /** Classes */
    public static final DeferredItem<Item> ARTIFICER = ITEMS.register("class/artificer", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BARBARIAN = ITEMS.register("class/barbarian", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BARD = ITEMS.register("class/bard", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DRUID = ITEMS.register("class/druid", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> FIGHTER = ITEMS.register("class/fighter", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MONK = ITEMS.register("class/monk", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RANGER = ITEMS.register("class/ranger", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ROGUE = ITEMS.register("class/rogue", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SORCERER = ITEMS.register("class/sorcerer", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> WARLOCK = ITEMS.register("class/warlock", () -> new Item(new Item.Properties()));


    /** Subclasses */
    public static final DeferredItem<Item> ALCHEMIST = ITEMS.register("subclass/artificer/alchemist", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BATTLE_SMITH = ITEMS.register("subclass/artificer/battle_smith", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> VALOR = ITEMS.register("subclass/bard/valor", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> LIFE = ITEMS.register("subclass/cleric/life", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TEMPEST = ITEMS.register("subclass/cleric/tempest", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TRICKERY = ITEMS.register("subclass/cleric/trickery", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> WAR = ITEMS.register("subclass/cleric/war", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> LAND = ITEMS.register("subclass/druid/land", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MOON = ITEMS.register("subclass/druid/moon", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SPORES = ITEMS.register("subclass/druid/spores", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> BATTLE_MASTER = ITEMS.register("subclass/fighter/battle_master", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CHAMPION = ITEMS.register("subclass/fighter/champion", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ELDRITCH_KNIGHT = ITEMS.register("subclass/fighter/eldritch_knight", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> WILDHEART = ITEMS.register("subclass/barbarian/wildheart", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BARBARIAN_WILD_MAGIC = ITEMS.register("subclass/barbarian/wild_magic", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> FOUR_ELEMENTS = ITEMS.register("subclass/monk/four_elements", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> OPEN_HAND = ITEMS.register("subclass/monk/open_hand", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ANCIENTS = ITEMS.register("subclass/paladin/ancients", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DEVOTION = ITEMS.register("subclass/paladin/devotion", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> VENGEANCE = ITEMS.register("subclass/paladin/vengeance", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> BEAST_MASTER = ITEMS.register("subclass/ranger/beast_master", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> GLOOM_STALKER = ITEMS.register("subclass/ranger/gloom_stalker", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> HUNTER = ITEMS.register("subclass/ranger/hunter", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ASSASSIN = ITEMS.register("subclass/rogue/assassin", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> THIEF = ITEMS.register("subclass/rogue/thief", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ARCANE_TRICKSTER = ITEMS.register("subclass/rogue/arcane_trickster", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> DRACONIC_BLOODLINE = ITEMS.register("subclass/sorcerer/draconic_bloodline", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> GREAT_OLD_ONE = ITEMS.register("subclass/warlock/great_old_one", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> FIEND = ITEMS.register("subclass/warlock/fiend", () -> new Item(new Item.Properties()));

    public static final DeferredItem<Item> ABJURATION = ITEMS.register("subclass/wizard/abjuration", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CONJURATION = ITEMS.register("subclass/wizard/conjuration", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> EVOCATION = ITEMS.register("subclass/wizard/evocation", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> NECROMANCY = ITEMS.register("subclass/wizard/necromancy", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TRANSMUTATION = ITEMS.register("subclass/wizard/transmutation", () -> new Item(new Item.Properties()));

    /** Feats */
    public static final DeferredItem<Item> CHEF = ITEMS.register("feats/chef", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DUAL_WIELDER = ITEMS.register("feats/dual_wielder", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ELEMENTAL_ADEPT = ITEMS.register("feats/elemental_adept", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LUCKY = ITEMS.register("feats/lucky", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MAGE_SLAYER = ITEMS.register("feats/mage_slayer", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MARTIAL_ADEPT = ITEMS.register("feats/martial_adept", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> POLEARM_MASTER = ITEMS.register("feats/polearm_master", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SHIELD_MASTER = ITEMS.register("feats/shield_master", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TOUGH = ITEMS.register("feats/tough", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SKILLED = ITEMS.register("feats/skilled", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MOBILE = ITEMS.register("feats/mobile", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DEFENSIVE_DUELIST = ITEMS.register("feats/defensive_duelist", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> SAVAGE_ATTACKER = ITEMS.register("feats/savage_attacker", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> RESILIENT = ITEMS.register("feats/resilient", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TWO_HANDED = ITEMS.register("feats/two_handed", () -> new Item(new Item.Properties()));

    /** Apts */
    public static final DeferredItem<Item> BLD = ITEMS.register("apts/bld", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> STR = ITEMS.register("apts/str", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DEF = ITEMS.register("apts/def", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DEX = ITEMS.register("apts/dex", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> INT = ITEMS.register("apts/int", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CONST = ITEMS.register("apts/const", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LCK = ITEMS.register("apts/lck", () -> new Item(new Item.Properties()));

    /** Actual Items */
    public static final DeferredItem<Item> GOODBERRY = ITEMS.register("goodberry", () -> new GoodberryItem(new Item.Properties()));
    public static final DeferredItem<Item> MELEE_RUNE = ITEMS.register("melee_rune", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> ARCHERY_RUNE = ITEMS.register("archery_rune", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MELEE_UPGRADE_ORB = ITEMS.register("melee_upgrade_orb",
            () -> new UpgradeOrbItem(new Item.Properties().rarity(Rarity.UNCOMMON)
                    .component(ComponentRegistry.UPGRADE_ORB_TYPE, ModUpgradeOrbTypes.MELEE_DAMAGE)));
    public static final DeferredItem<Item> ARCHERY_UPGRADE_ORB = ITEMS.register("archery_upgrade_orb",
            () -> new UpgradeOrbItem(new Item.Properties().rarity(Rarity.UNCOMMON)
                    .component(ComponentRegistry.UPGRADE_ORB_TYPE, ModUpgradeOrbTypes.ARROW_DAMAGE)));
    public static final DeferredItem<Item> ORB_OF_ANCESTRY = ITEMS.register("orb_of_ancestry",
            () -> new ReselectionOrbItem(new Item.Properties().rarity(Rarity.UNCOMMON), SelectionLayers.ANCESTRY));
    public static final DeferredItem<Item> ORB_OF_VOCATION = ITEMS.register("orb_of_vocation",
            () -> new ReselectionOrbItem(new Item.Properties().rarity(Rarity.UNCOMMON), SelectionLayers.VOCATION));


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
