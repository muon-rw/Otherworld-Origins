package dev.muon.raven_dnd_origins.util.shapeshift;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.power.ShapeshiftPower;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.function.BiConsumer;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * Wild shape keeps armor equipped but strips every attribute modifier it contributes: innate item
 * modifiers, enchantment attribute effects and anything other mods attach through the item
 * attribute event, such as Apotheosis affixes and gems. Non-attribute equipment behaviour keeps
 * working because the pieces never leave their slots. {@code LivingEntityMixin} stops the
 * equipment scan from re-adding armor modifiers while a suppressing form is active.
 */
@EventBusSubscriber(modid = RavenDndOrigins.MODID)
public class ShapeshiftEquipmentHandler {
    private static final int RESWEEP_INTERVAL_TICKS = 40;

    public static boolean suppressesArmorModifiers(Player player) {
        ShapeshiftPower.Configuration config = ShapeshiftPower.getActiveShapeshiftConfig(player);
        return config != null && config.suppressArmorModifiers();
    }

    public static void suppressArmorModifiers(ServerPlayer player) {
        forEachArmorModifier(player, (instance, modifier) -> instance.removeModifier(modifier.id()));
        // Location-triggered enchantment attributes (Soul Speed) live outside the modifier scan
        forEachArmorStack(player, (slot, stack) -> EnchantmentHelper.stopLocationBasedEffects(stack, player, slot));
    }

    public static void restoreArmorModifiers(ServerPlayer player) {
        forEachArmorModifier(player, (instance, modifier) -> {
            instance.removeModifier(modifier.id());
            instance.addTransientModifier(modifier);
        });
        forEachArmorStack(player, (slot, stack) ->
                EnchantmentHelper.runLocationChangedEffects(player.serverLevel(), stack, player, slot));
    }

    private static void forEachArmorStack(ServerPlayer player, BiConsumer<EquipmentSlot, ItemStack> action) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!slot.isArmor()) {
                continue;
            }
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                action.accept(slot, stack);
            }
        }
    }

    private static void forEachArmorModifier(ServerPlayer player, BiConsumer<AttributeInstance, AttributeModifier> action) {
        AttributeMap attributes = player.getAttributes();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!slot.isArmor()) {
                continue;
            }
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            stack.forEachModifier(slot, (attribute, modifier) -> {
                AttributeInstance instance = attributes.getInstance(attribute);
                if (instance != null) {
                    action.accept(instance, modifier);
                }
            });
        }
    }

    /** Safety net for modifiers re-added by paths that bypass the equipment scan. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % RESWEEP_INTERVAL_TICKS != 0) {
            return;
        }
        if (suppressesArmorModifiers(player)) {
            suppressArmorModifiers(player);
        }
    }
}
