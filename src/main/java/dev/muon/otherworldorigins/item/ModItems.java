package dev.muon.otherworldorigins.item;

import dev.muon.otherworldorigins.OtherworldOrigins;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, OtherworldOrigins.MODID);


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
    public static final RegistryObject<Item> SKELETON_PORTRAIT = ITEMS.register("portrait/skeleton", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STOUT_PORTRAIT = ITEMS.register("portrait/stout", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> TIEFLING_PORTRAIT = ITEMS.register("portrait/tiefling", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> UNDEAD_PORTRAIT = ITEMS.register("portrait/undead", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> VARIANT_PORTRAIT = ITEMS.register("portrait/variant", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WHITE_PORTRAIT = ITEMS.register("portrait/white", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WITHER_SKELETON_PORTRAIT = ITEMS.register("portrait/wither_skeleton", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WOOD_PORTRAIT = ITEMS.register("portrait/wood", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ZOMBIE_PORTRAIT = ITEMS.register("portrait/zombie", () -> new Item(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}