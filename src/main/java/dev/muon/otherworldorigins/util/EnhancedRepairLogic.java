package dev.muon.otherworldorigins.util;

import dev.muon.otherworldorigins.power.EnhancedRepairPower;
import dev.muon.otherworldorigins.power.ModPowers;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredItemCondition;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.ItemAttributeModifierEvent;

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
        IPowerContainer container = ApoliAPI.getPowerContainer(player);
        if (container == null) {
            return;
        }
        Level level = player.level();
        for (var holder : container.getPowers(ModPowers.ENHANCED_REPAIR.get())) {
            var power = holder.value();
            if (!power.isActive(player)) {
                continue;
            }
            EnhancedRepairPower.Configuration cfg = power.getConfiguration();
            if (ConfiguredItemCondition.check(cfg.itemCondition(), level, result)) {
                MasterworkAffixNbt.putMasterwork(result, cfg.attribute(), cfg.operation(), cfg.value());
                break;
            }
        }
    }

    public static void onItemAttributeModifiers(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        Attribute attr = MasterworkAffixNbt.readAttribute(stack);
        if (attr == null) {
            return;
        }
        AttributeModifier modifier = MasterworkAffixNbt.readAsModifier(stack);
        if (modifier == null) {
            return;
        }
        EquipmentSlot stackSlot = Mob.getEquipmentSlotForItem(stack);
        if (event.getSlotType() != stackSlot) {
            return;
        }
        event.addModifier(attr, modifier);
    }
}
