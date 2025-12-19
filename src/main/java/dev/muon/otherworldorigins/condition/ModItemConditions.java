package dev.muon.otherworldorigins.condition;

import dev.muon.otherworldorigins.OtherworldOrigins;
import io.github.apace100.apoli.power.factory.condition.ConditionFactory;
import io.github.apace100.apoli.registry.ApoliRegistries;
import io.github.apace100.calio.data.SerializableData;
import io.redspace.ironsspellbooks.item.CastingItem;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.fml.ModList;

public class ModItemConditions {
    public static void register() {
        register(new ConditionFactory<>(OtherworldOrigins.loc("is_melee_weapon"), new SerializableData(), (data, stack) ->
                Enchantments.SHARPNESS.canEnchant(stack) || stack.getItem() instanceof DiggerItem && ((DiggerItem) stack.getItem()).getAttackDamage() > 0
        ));

        register(new ConditionFactory<>(OtherworldOrigins.loc("is_bow"), new SerializableData(), (data, stack) ->
                stack.getItem() instanceof BowItem
        ));

        register(new ConditionFactory<>(OtherworldOrigins.loc("is_one_handed"), new SerializableData(), (data, stack) -> {
            if (ModList.get().isLoaded("bettercombat")) {
                WeaponAttributes weaponAttributes = WeaponRegistry.getAttributes(stack);
                return weaponAttributes != null && !weaponAttributes.isTwoHanded();
            }
            return true;
        }));

        register(new ConditionFactory<>(OtherworldOrigins.loc("is_two_handed"), new SerializableData(), (data, stack) -> {
            if (ModList.get().isLoaded("bettercombat")) {
                WeaponAttributes weaponAttributes = WeaponRegistry.getAttributes(stack);
                return weaponAttributes != null && weaponAttributes.isTwoHanded();
            }
            return false;
        }));

        register(new ConditionFactory<>(OtherworldOrigins.loc("can_cast"), new SerializableData(), (data, stack) ->
                stack.getItem() instanceof CastingItem
        ));

        register(new ConditionFactory<>(OtherworldOrigins.loc("is_sword"), new SerializableData(), (data, stack) -> {
            String itemName = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            return (stack.getItem() instanceof SwordItem || Enchantments.SHARPNESS.canEnchant(stack))
                    && (itemName.contains("sword"));
        }));

        register(new ConditionFactory<>(OtherworldOrigins.loc("is_staff"), new SerializableData(), (data, stack) ->
                BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().contains("staff")
        ));

        register(new ConditionFactory<>(OtherworldOrigins.loc("is_fist_weapon"), new SerializableData(), (data, stack) -> {
            String itemName = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            return (stack.getItem() instanceof SwordItem || Enchantments.SHARPNESS.canEnchant(stack))
                    && (itemName.contains("fist") || itemName.contains("claw") || itemName.contains("gauntlet"));
        }));

        register(new ConditionFactory<>(OtherworldOrigins.loc("is_tool"), new SerializableData(), (data, stack) ->
                stack.getItem() instanceof DiggerItem || Enchantments.BLOCK_EFFICIENCY.canEnchant(stack)
        ));

        register(new ConditionFactory<>(OtherworldOrigins.loc("is_golden_armor"), new SerializableData(), (data, stack) -> {
            String itemName = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            return (stack.getItem() instanceof ArmorItem && (itemName.contains("gold") || itemName.contains("gilded")));
        }));

        register(new ConditionFactory<>(OtherworldOrigins.loc("is_golden_weapon"), new SerializableData(), (data, stack) -> {
            String itemName = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            return (Enchantments.SHARPNESS.canEnchant(stack) && (itemName.contains("gold") || itemName.contains("gilded")));
        }));

        register(new ConditionFactory<>(OtherworldOrigins.loc("is_golden_tool"), new SerializableData(), (data, stack) -> {
            String itemName = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            return (stack.getItem() instanceof DiggerItem && (itemName.contains("gold") || itemName.contains("gilded")));
        }));
    }

    private static void register(ConditionFactory<net.minecraft.world.item.ItemStack> conditionFactory) {
        Registry.register(ApoliRegistries.ITEM_CONDITION, conditionFactory.getSerializerId(), conditionFactory);
    }
}

