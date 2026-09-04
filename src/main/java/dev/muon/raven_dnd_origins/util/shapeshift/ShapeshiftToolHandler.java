package dev.muon.raven_dnd_origins.util.shapeshift;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.mixin.MobAccessor;
import dev.muon.raven_dnd_origins.power.ShapeshiftPower;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = RavenDndOrigins.MODID)
public class ShapeshiftToolHandler {

    private static final Map<EntityType<?>, SoundEvent> ATTACK_SOUND_CACHE = new HashMap<>();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        ShapeshiftPower.Configuration config = getShapeshiftConfig(player);
        if (config == null) return;

        if (!config.allowTools() && isWeaponOrTool(player.getMainHandItem())) {
            event.setCanceled(true);
            notifyBlocked(player);
            return;
        }

        if (config.playAttackSoundChance() > 0 && player.getRandom().nextFloat() < config.playAttackSoundChance()) {
            playMorphAttackSound(player, config.entityType());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (isBlockedHeldItem(player)) {
            event.setCanceled(true);
            notifyBlocked(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (isBlockedHeldItem(player)) {
            event.setCanceled(true);
            notifyBlocked(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (isBlockedHeldItem(player)) {
            event.setCanceled(true);
            notifyBlocked(player);
        }
    }

    public static boolean isBlockedHeldItem(Player player) {
        if (player == null) return false;
        ShapeshiftPower.Configuration config = getShapeshiftConfig(player);
        return config != null && !config.allowTools() && isWeaponOrTool(player.getMainHandItem());
    }

    public static boolean isWeaponOrTool(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        if (item instanceof TieredItem) return true;
        if (item instanceof ProjectileWeaponItem) return true;
        if (item instanceof TridentItem) return true;
        for (ItemAttributeModifiers.Entry entry : stack.getAttributeModifiers().modifiers()) {
            if (entry.slot().test(EquipmentSlot.MAINHAND) && entry.attribute().value() == Attributes.ATTACK_DAMAGE.value()) {
                return true;
            }
        }
        return false;
    }

    public static void notifyBlocked(Player player) {
        player.displayClientMessage(
                Component.translatable("message.raven_dnd_origins.shapeshift_tool_blocked")
                        .withStyle(ChatFormatting.RED),
                true
        );
    }

    @Nullable
    private static ShapeshiftPower.Configuration getShapeshiftConfig(Player player) {
        if (player == null) return null;
        return ShapeshiftPower.getActiveShapeshiftConfig(player);
    }

    private static void playMorphAttackSound(Player player, ResourceLocation entityTypeId) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(entityTypeId);
        if (type == null) return;

        SoundEvent sound;
        if (ATTACK_SOUND_CACHE.containsKey(type)) {
            sound = ATTACK_SOUND_CACHE.get(type);
        } else {
            sound = resolveAmbientSound(player.level(), type);
            ATTACK_SOUND_CACHE.put(type, sound);
        }
        if (sound != null) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    sound, SoundSource.PLAYERS, 1.0F, 0.9F + player.getRandom().nextFloat() * 0.2F);
        }
    }

    @Nullable
    private static SoundEvent resolveAmbientSound(Level level, EntityType<?> type) {
        Entity temp = type.create(level);
        if (temp instanceof Mob mob) {
            SoundEvent sound = ((MobAccessor) mob).invokeGetAmbientSound();
            temp.discard();
            return sound;
        }
        if (temp != null) temp.discard();
        return null;
    }
}
