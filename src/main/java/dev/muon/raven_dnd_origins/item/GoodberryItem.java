package dev.muon.raven_dnd_origins.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/**
 * Berries that restore 4 hearts when eaten (infused). Lose their magic after one Minecraft day (24000 ticks).
 * Infused berries heal 8 health directly and are always edible; decayed berries provide minimal nutrition only.
 */
public class GoodberryItem extends Item {

    private static final String CREATED_AT_KEY = "CreatedAt";
    private static final String DECAYED_KEY = "Decayed";
    private static final int INFUSED_HEAL = 8;
    /** One Minecraft day in ticks */
    public static final long DECAY_TICKS = 24000L;

    private static final FoodProperties BASE_FOOD = new FoodProperties.Builder()
            .nutrition(2)
            .saturationModifier(0.1F)
            .fast()
            .build();

    private static final FoodProperties INFUSED_FOOD = new FoodProperties.Builder()
            .nutrition(2)
            .saturationModifier(0.1F)
            .fast()
            .alwaysEdible()
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

    public static long getRemainingTicks(ItemStack stack, Level level) {
        CompoundTag tag = customData(stack);
        if (!tag.contains(CREATED_AT_KEY)) return DECAY_TICKS;
        return Math.max(0, tag.getLong(CREATED_AT_KEY) + DECAY_TICKS - level.getGameTime());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return !isDecayedCached(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (level.isClientSide) return;

        boolean decayed = isDecayed(stack, level);
        CompoundTag current = customData(stack);
        // Stacks from give action, loot tables, creative etc. have no CreatedAt: set it on first inventory tick
        if (current.contains(CREATED_AT_KEY) && current.getBoolean(DECAYED_KEY) == decayed) {
            return;
        }
        long createdAt = current.contains(CREATED_AT_KEY) ? current.getLong(CREATED_AT_KEY) : level.getGameTime();
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putLong(CREATED_AT_KEY, createdAt);
            tag.putBoolean(DECAYED_KEY, decayed);
        });
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
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                tag.putLong(CREATED_AT_KEY, worldTime);
                tag.remove(DECAYED_KEY); // Fresh berries are infused
            });
        }
    }

    public static boolean isDecayed(ItemStack stack, Level level) {
        CompoundTag tag = customData(stack);
        if (!tag.contains(CREATED_AT_KEY)) {
            return false; // No timestamp = treat as fresh (e.g. creative menu, legacy)
        }
        return level.getGameTime() - tag.getLong(CREATED_AT_KEY) >= DECAY_TICKS;
    }

    /** Uses the Decayed flag cached by inventoryTick; defaults to infused (false) if not yet cached. */
    public static boolean isDecayedCached(ItemStack stack) {
        return customData(stack).getBoolean(DECAYED_KEY);
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        super.onCraftedBy(stack, level, player);
        if (!level.isClientSide && !customData(stack).contains(CREATED_AT_KEY)) {
            setCreationTime(stack, level.getGameTime());
        }
    }

    private static CompoundTag customData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
}
