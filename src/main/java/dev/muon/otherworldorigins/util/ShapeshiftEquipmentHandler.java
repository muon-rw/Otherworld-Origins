package dev.muon.otherworldorigins.util;

import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.power.ShapeshiftPower;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OtherworldOrigins.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ShapeshiftEquipmentHandler {

    /** Right-click equip prevention. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player == null) return;

        EquipmentSlot slot = Mob.getEquipmentSlotForItem(event.getItemStack());
        if (!slot.isArmor()) return;

        ShapeshiftPower.Configuration config = ShapeshiftPower.getActiveShapeshiftConfig(player);
        if (config != null && config.preventEquipment()) {
            event.setCanceled(true);
            player.displayClientMessage(
                    Component.translatable("tooltip.otherworldorigins.equipment_prevented")
                            .withStyle(ChatFormatting.RED),
                    true
            );
        }
    }

    /** 40-tick fallback: catches anything the specific hooks miss. */
    @SubscribeEvent
    public static void onPlayerTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;
        if (player.tickCount % 40 != 0) return;

        ShapeshiftPower.Configuration config = ShapeshiftPower.getActiveShapeshiftConfig(player);
        if (config == null || !config.preventEquipment()) return;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!slot.isArmor()) continue;
            ItemStack armor = player.getItemBySlot(slot);
            if (armor.isEmpty()) continue;

            player.setItemSlot(slot, ItemStack.EMPTY);
            if (!player.getInventory().add(armor)) {
                player.drop(armor, false);
            }
        }
    }
}
