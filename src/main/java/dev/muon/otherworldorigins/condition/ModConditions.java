package dev.muon.otherworldorigins.condition;

import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.condition.entity.AnyOnLayerCondition;
import dev.muon.otherworldorigins.condition.entity.HasSkillCondition;
import dev.muon.otherworldorigins.condition.entity.LeveledChanceCondition;
import dev.muon.otherworldorigins.condition.entity.ManaCondition;
import dev.muon.otherworldorigins.condition.entity.PlayerLevelCondition;
import io.github.edwinmindcraft.apoli.api.power.factory.*;
import io.github.edwinmindcraft.apoli.api.registry.ApoliRegistries;
import io.github.edwinmindcraft.apoli.common.condition.bientity.SimpleBiEntityCondition;
import io.github.edwinmindcraft.apoli.common.condition.entity.SimpleEntityCondition;
import io.github.edwinmindcraft.apoli.common.condition.item.SimpleItemCondition;
import io.redspace.ironsspellbooks.item.CastingItem;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;


public class ModConditions {
    public static final DeferredRegister<EntityCondition<?>> ENTITY_CONDITIONS = DeferredRegister.create(ApoliRegistries.ENTITY_CONDITION_KEY, OtherworldOrigins.MODID);
    public static final DeferredRegister<BiEntityCondition<?>> BIENTITY_CONDITIONS = DeferredRegister.create(ApoliRegistries.BIENTITY_CONDITION_KEY, OtherworldOrigins.MODID);
    public static final DeferredRegister<BlockCondition<?>> BLOCK_CONDITIONS = DeferredRegister.create(ApoliRegistries.BLOCK_CONDITION_KEY, OtherworldOrigins.MODID);
    public static final DeferredRegister<ItemCondition<?>> ITEM_CONDITIONS = DeferredRegister.create(ApoliRegistries.ITEM_CONDITION_KEY, OtherworldOrigins.MODID);

    /**Entity*/
    public static final RegistryObject<SimpleEntityCondition> IS_ARROW = ENTITY_CONDITIONS.register("is_arrow", () ->
            new SimpleEntityCondition(entity -> entity instanceof AbstractArrow));

    public static final RegistryObject<SimpleEntityCondition> CREATIVE_MODE = ENTITY_CONDITIONS.register("creative_mode", () ->
            new SimpleEntityCondition(entity ->
                    entity instanceof Player player && player.getAbilities().instabuild));

    public static final RegistryObject<PlayerLevelCondition> PLAYER_LEVEL = ENTITY_CONDITIONS.register("player_level", PlayerLevelCondition::new);
    public static final RegistryObject<AnyOnLayerCondition> ANY_ON_LAYER = ENTITY_CONDITIONS.register("any_on_layer", AnyOnLayerCondition::new);
    public static final RegistryObject<ManaCondition> PLAYER_MANA = ENTITY_CONDITIONS.register("player_mana", ManaCondition::new);
    public static final RegistryObject<HasSkillCondition> HAS_SKILL = ENTITY_CONDITIONS.register("has_skill", HasSkillCondition::new);
    public static final RegistryObject<LeveledChanceCondition> LEVELED_CHANCE = ENTITY_CONDITIONS.register("leveled_chance", LeveledChanceCondition::new);




    /**Bientity*/
    public static final RegistryObject<SimpleBiEntityCondition> IS_ALLIED = BIENTITY_CONDITIONS.register("allied", () ->
            new SimpleBiEntityCondition((actor, target) -> actor.isAlliedTo(target)));

    /**Item*/
    public static final RegistryObject<SimpleItemCondition> IS_MELEE_WEAPON = ITEM_CONDITIONS.register("is_melee_weapon", () ->
            new SimpleItemCondition(stack ->
                    Enchantments.SHARPNESS.canEnchant(stack) || stack.getItem() instanceof DiggerItem && ((DiggerItem) stack.getItem()).getAttackDamage() > 0)
    );
    public static final RegistryObject<SimpleItemCondition> IS_BOW = ITEM_CONDITIONS.register("is_bow", () ->
            new SimpleItemCondition(stack ->
                    stack.getItem() instanceof BowItem)
    );

    public static final RegistryObject<SimpleItemCondition> IS_ONE_HANDED = ITEM_CONDITIONS.register("is_one_handed", () ->
            new SimpleItemCondition(stack -> {
                if (ModList.get().isLoaded("bettercombat")) {
                    WeaponAttributes weaponAttributes = WeaponRegistry.getAttributes(stack);
                    return weaponAttributes != null && !weaponAttributes.isTwoHanded();
                }
                return true;
            })
    );

    public static final RegistryObject<SimpleItemCondition> IS_TWO_HANDED = ITEM_CONDITIONS.register("is_two_handed", () ->
            new SimpleItemCondition(stack -> {
                if (ModList.get().isLoaded("bettercombat")) {
                    WeaponAttributes weaponAttributes = WeaponRegistry.getAttributes(stack);
                    return weaponAttributes != null && weaponAttributes.isTwoHanded();
                }
                return false;
            })
    );

    public static final RegistryObject<SimpleItemCondition> CAN_CAST = ITEM_CONDITIONS.register("can_cast", () ->
            new SimpleItemCondition(stack ->
               stack.getItem() instanceof CastingItem
            )
    );

    public static final RegistryObject<SimpleItemCondition> IS_SWORD = ITEM_CONDITIONS.register("is_sword", () ->
            new SimpleItemCondition(stack -> {
                String itemName = ForgeRegistries.ITEMS.getKey(stack.getItem()).getPath();
                return (stack.getItem() instanceof SwordItem || Enchantments.SHARPNESS.canEnchant(stack))
                        && (itemName.contains("sword"));
            })
    );

    public static final RegistryObject<SimpleItemCondition> IS_STAFF = ITEM_CONDITIONS.register("is_staff", () ->
            new SimpleItemCondition(stack -> ForgeRegistries.ITEMS.getKey(stack.getItem()).getPath().contains("staff"))
    );

    public static final RegistryObject<SimpleItemCondition> IS_FIST_WEAPON = ITEM_CONDITIONS.register("is_fist_weapon", () ->
            new SimpleItemCondition(stack -> {
                String itemName = ForgeRegistries.ITEMS.getKey(stack.getItem()).getPath();
                return (stack.getItem() instanceof SwordItem || Enchantments.SHARPNESS.canEnchant(stack))
                        && (itemName.contains("fist") || itemName.contains("claw") || itemName.contains("gauntlet"));
            })
    );
    public static final RegistryObject<SimpleItemCondition> IS_TOOL = ITEM_CONDITIONS.register("is_tool", () ->
            new SimpleItemCondition(stack ->
                    stack.getItem() instanceof DiggerItem || Enchantments.BLOCK_EFFICIENCY.canEnchant(stack) )
    );

    public static final RegistryObject<SimpleItemCondition> IS_GOLDEN_ARMOR = ITEM_CONDITIONS.register("is_golden_armor", () ->
            new SimpleItemCondition(stack -> {
                String itemName = ForgeRegistries.ITEMS.getKey(stack.getItem()).getPath();
                return (stack.getItem() instanceof ArmorItem && (itemName.contains("gold") || itemName.contains("gilded")));
            })
    );
    public static final RegistryObject<SimpleItemCondition> IS_GOLDEN_WEAPON = ITEM_CONDITIONS.register("is_golden_weapon", () ->
            new SimpleItemCondition(stack -> {
                String itemName = ForgeRegistries.ITEMS.getKey(stack.getItem()).getPath();
                return (Enchantments.SHARPNESS.canEnchant(stack) && (itemName.contains("gold") || itemName.contains("gilded")));
            })
    );
    public static final RegistryObject<SimpleItemCondition> IS_GOLDEN_TOOL = ITEM_CONDITIONS.register("is_golden_tool", () ->
            new SimpleItemCondition(stack -> {
                String itemName = ForgeRegistries.ITEMS.getKey(stack.getItem()).getPath();
                return (stack.getItem() instanceof DiggerItem && (itemName.contains("gold") || itemName.contains("gilded")));
            })
    );

    public static void register(IEventBus eventBus) {
        ENTITY_CONDITIONS.register(eventBus);
        BIENTITY_CONDITIONS.register(eventBus);
        BLOCK_CONDITIONS.register(eventBus);
        ITEM_CONDITIONS.register(eventBus);
    }
}
