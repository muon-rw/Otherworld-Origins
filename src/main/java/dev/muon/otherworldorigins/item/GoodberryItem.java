package dev.muon.otherworldorigins.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Berries that restore 4 hearts when eaten (infused). Lose their magic after one Minecraft day (24000 ticks).
 * Infused berries heal 8 health directly and are always edible; decayed berries provide minimal nutrition only.
 */
public class GoodberryItem extends Item {

    private static final String CREATED_AT_KEY = "CreatedAt";
    private static final String DECAYED_KEY = "Decayed";
    private static final int INFUSED_HEAL = 8;
    /** One Minecraft day in ticks */
    private static final long DECAY_TICKS = 24000L;

    private static final FoodProperties BASE_FOOD = new FoodProperties.Builder()
            .nutrition(2)
            .saturationMod(0.1F)
            .fast()
            .build();

    private static final FoodProperties INFUSED_FOOD = new FoodProperties.Builder()
            .nutrition(2)
            .saturationMod(0.1F)
            .fast()
            .alwaysEat()
            .build();

    public GoodberryItem(Properties properties) {
        super(properties.food(BASE_FOOD));
    }

    @Override
    public FoodProperties getFoodProperties(ItemStack stack, LivingEntity entity) {
        boolean decayed = entity != null ? isDecayed(stack, entity.level()) : isDecayedCached(stack);
        return decayed ? BASE_FOOD : INFUSED_FOOD;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof Player player && !isDecayed(stack, level)) {
            player.heal(INFUSED_HEAL);
        }
        return super.finishUsingItem(stack, level, entity);
    }

    @Override
    public SoundEvent getEatingSound() {
        return SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltipComponents, TooltipFlag flag) {
        boolean decayed = level != null ? isDecayed(stack, level) : isDecayedCached(stack);
        var darkGrayItalic = Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withItalic(true);
        if (decayed) {
            tooltipComponents.add(Component.translatable("item.otherworldorigins.goodberry.tooltip.ordinary")
                    .withStyle(darkGrayItalic));
        } else if (level != null) {
            long remainingTicks = getRemainingTicks(stack, level);
            String timeStr = formatTicksToTime(remainingTicks);
            tooltipComponents.add(Component.translatable("item.otherworldorigins.goodberry.tooltip.decays_in", timeStr)
                    .withStyle(darkGrayItalic));
        }
    }

    private static long getRemainingTicks(ItemStack stack, Level level) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(CREATED_AT_KEY)) return DECAY_TICKS;
        long createdAt = tag.getLong(CREATED_AT_KEY);
        return Math.max(0, createdAt + DECAY_TICKS - level.getGameTime());
    }

    private static String formatTicksToTime(long ticks) {
        long seconds = ticks / 20;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        if (hours > 0) {
            return hours + "h " + minutes + "m " + secs + "s";
        }
        if (minutes > 0) {
            return minutes + "m " + secs + "s";
        }
        return secs + "s";
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return !isDecayedCached(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide) return;

        CompoundTag tag = stack.getOrCreateTag();
        // Stacks from give action, loot tables, creative etc. have no CreatedAt - set it on first inventory tick
        if (!tag.contains(CREATED_AT_KEY)) {
            tag.putLong(CREATED_AT_KEY, level.getGameTime());
        }
        boolean decayed = isDecayed(stack, level);
        tag.putBoolean(DECAYED_KEY, decayed);
    }

    /**
     * Creates a goodberry stack. When given to a player or placed in an inventory, {@link #inventoryTick}
     * will set CreatedAt automatically. Use {@link #setCreationTime} only when you need a specific timestamp.
     */
    public static ItemStack createFreshStack(int count) {
        return new ItemStack(ModItems.GOODBERRY.get(), count);
    }

    /**
     * Sets the creation time for a goodberry stack. Call when giving berries to a player.
     */
    public static void setCreationTime(ItemStack stack, long worldTime) {
        if (stack.getItem() instanceof GoodberryItem) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putLong(CREATED_AT_KEY, worldTime);
            tag.remove(DECAYED_KEY); // Fresh berries are infused
        }
    }

    public static boolean isDecayed(ItemStack stack, Level level) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(CREATED_AT_KEY)) {
            return false; // No timestamp = treat as fresh (e.g. creative menu, legacy)
        }
        long createdAt = tag.getLong(CREATED_AT_KEY);
        return level.getGameTime() - createdAt >= DECAY_TICKS;
    }

    /** Uses cached Decayed tag from inventoryTick; defaults to infused (false) if not yet cached. */
    private static boolean isDecayedCached(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(DECAYED_KEY);
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, net.minecraft.world.entity.player.Player player) {
        super.onCraftedBy(stack, level, player);
        if (!level.isClientSide && !stack.getOrCreateTag().contains(CREATED_AT_KEY)) {
            setCreationTime(stack, level.getGameTime());
        }
    }
}
