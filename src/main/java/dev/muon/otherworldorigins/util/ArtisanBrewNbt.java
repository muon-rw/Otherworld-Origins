package dev.muon.otherworldorigins.util;

import dev.muon.otherworldorigins.OtherworldOrigins;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Sibling tag to {@link MasterworkAffixNbt} under the same {@code otherworldorigins:item_affinities}
 * subroot. Stores per-category duration/amplifier bonuses applied to potions when an Alchemist
 * artificer crafts them. Read at consumption by {@code LivingEntityMixin} and at tooltip render
 * by {@code PotionUtilsMixin} — never overwrites vanilla {@code Potion} or {@code CustomPotionEffects}.
 */
public final class ArtisanBrewNbt {

    public static final String SUBKEY = "artisan_brew";
    public static final String BREWER_UUID_KEY = "brewer_uuid";
    public static final String BENEFICIAL_KEY = "beneficial";
    public static final String HARMFUL_KEY = "harmful";
    public static final String NEUTRAL_KEY = "neutral";
    public static final String DURATION_MULT_KEY = "duration_mult";
    public static final String AMPLIFIER_ADD_KEY = "amplifier_add";

    private ArtisanBrewNbt() {
    }

    public static boolean has(ItemStack stack) {
        return getTag(stack) != null;
    }

    @Nullable
    public static CompoundTag getTag(ItemStack stack) {
        if (!stack.hasTag()) return null;
        CompoundTag root = stack.getTagElement(OtherworldOrigins.MODID);
        if (root == null) return null;
        if (!root.contains(MasterworkAffixNbt.MOD_SUBROOT, Tag.TAG_COMPOUND)) return null;
        CompoundTag aff = root.getCompound(MasterworkAffixNbt.MOD_SUBROOT);
        if (!aff.contains(SUBKEY, Tag.TAG_COMPOUND)) return null;
        CompoundTag tag = aff.getCompound(SUBKEY);
        return tag.isEmpty() ? null : tag;
    }

    /** Combined bonus for a given effect category. {@code (1.0, 0)} when absent. */
    public static Bonus getBonus(ItemStack stack, MobEffectCategory category) {
        CompoundTag tag = getTag(stack);
        if (tag == null) return Bonus.NONE;
        return readBonus(tag, categoryKey(category));
    }

    public static void write(ItemStack stack, @Nullable UUID brewerUuid, Bonus beneficial, Bonus harmful, Bonus neutral) {
        if (beneficial.isNone() && harmful.isNone() && neutral.isNone()) {
            return;
        }
        CompoundTag root = stack.getOrCreateTagElement(OtherworldOrigins.MODID);
        CompoundTag aff = root.getCompound(MasterworkAffixNbt.MOD_SUBROOT);
        CompoundTag tag = new CompoundTag();
        if (brewerUuid != null) {
            tag.put(BREWER_UUID_KEY, NbtUtils.createUUID(brewerUuid));
        }
        writeBonus(tag, BENEFICIAL_KEY, beneficial);
        writeBonus(tag, HARMFUL_KEY, harmful);
        writeBonus(tag, NEUTRAL_KEY, neutral);
        aff.put(SUBKEY, tag);
        root.put(MasterworkAffixNbt.MOD_SUBROOT, aff);
    }

    private static String categoryKey(MobEffectCategory category) {
        return switch (category) {
            case BENEFICIAL -> BENEFICIAL_KEY;
            case HARMFUL -> HARMFUL_KEY;
            case NEUTRAL -> NEUTRAL_KEY;
        };
    }

    private static Bonus readBonus(CompoundTag root, String key) {
        if (!root.contains(key, Tag.TAG_COMPOUND)) return Bonus.NONE;
        CompoundTag b = root.getCompound(key);
        float dur = b.contains(DURATION_MULT_KEY, Tag.TAG_FLOAT) ? b.getFloat(DURATION_MULT_KEY) : 1.0f;
        int amp = b.getInt(AMPLIFIER_ADD_KEY);
        return new Bonus(dur, amp);
    }

    private static void writeBonus(CompoundTag root, String key, Bonus bonus) {
        if (bonus.isNone()) return;
        CompoundTag b = new CompoundTag();
        b.putFloat(DURATION_MULT_KEY, bonus.durationMultiplier);
        b.putInt(AMPLIFIER_ADD_KEY, bonus.amplifierAdd);
        root.put(key, b);
    }

    public record Bonus(float durationMultiplier, int amplifierAdd) {
        public static final Bonus NONE = new Bonus(1.0f, 0);

        public boolean isNone() {
            return durationMultiplier == 1.0f && amplifierAdd == 0;
        }

        public Bonus combine(Bonus other) {
            return new Bonus(durationMultiplier * other.durationMultiplier, amplifierAdd + other.amplifierAdd);
        }
    }
}
