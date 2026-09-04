package dev.muon.raven_dnd_origins.util;

import dev.muon.raven_dnd_origins.power.EnhancedRepairPower;
import dev.muon.raven_dnd_origins.power.ModPowers;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

public final class EnhancedRepairLogic {

    private EnhancedRepairLogic() {
    }

    public static boolean isDurabilityRepair(ItemStack originalLeft, ItemStack result) {
        if (originalLeft.isEmpty() || result.isEmpty() || !originalLeft.isDamageableItem()) {
            return false;
        }
        return result.getDamageValue() < originalLeft.getDamageValue();
    }

    /**
     * Applies or upgrades masterwork on {@code result} when the player performs an anvil durability repair.
     */
    public static void onAnvilRepair(Player player, ItemStack originalLeft, ItemStack result) {
        if (player.level().isClientSide() || !isDurabilityRepair(originalLeft, result)) {
            return;
        }
        Level level = player.level();
        ItemCtx ctx = new ItemCtx(result, level, player);
        for (EnhancedRepairPower.Configuration cfg :
                PowerLookup.active(player, ModPowers.ENHANCED_REPAIR, EnhancedRepairPower.Configuration.class)) {
            if (cfg.itemCondition().test(ctx)) {
                MasterworkAffixNbt.putMasterwork(result, cfg.attribute(), cfg.operation().vanillaOperation(), cfg.value());
                break;
            }
        }
    }

    public static void onItemAttributeModifiers(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        Holder<Attribute> attr = MasterworkAffixNbt.readAttribute(stack);
        if (attr == null) {
            return;
        }
        AttributeModifier modifier = MasterworkAffixNbt.readAsModifier(stack);
        if (modifier == null) {
            return;
        }
        event.addModifier(attr, modifier, EquipmentSlotGroup.bySlot(equipmentSlotOf(stack)));
    }

    private static EquipmentSlot equipmentSlotOf(ItemStack stack) {
        EquipmentSlot explicit = stack.getEquipmentSlot();
        if (explicit != null) {
            return explicit;
        }
        Equipable equipable = Equipable.get(stack);
        return equipable != null ? equipable.getEquipmentSlot() : EquipmentSlot.MAINHAND;
    }
}
