package dev.muon.otherworldorigins.util;

import dev.muon.otherworldorigins.OtherworldOrigins;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Emulates Apotheosis {@code AttributeAffix} / {@code GemItem} modifier identity: persistent UUIDs
 * under a dedicated item subtag plus encoded attribute operation state. Does not depend on Apotheosis APIs.
 */
public final class MasterworkAffixNbt {

    /** Mod-scoped root; keeps data out of {@code affix_data} to avoid colliding with Apotheosis UUID pools. */
    public static final String MOD_SUBROOT = "item_affinities";
    public static final String MASTERWORK_KEY = "masterwork";
    /** Same key name Apotheosis uses for cached modifier UUID lists. */
    public static final String UUIDS_KEY = "uuids";
    public static final String ATTRIBUTE_KEY = "attribute";
    public static final String OPERATION_KEY = "operation";
    public static final String VALUE_KEY = "value";

    public static final ResourceLocation MASTERWORK_AFFIX_ID = OtherworldOrigins.loc("masterwork");

    private MasterworkAffixNbt() {
    }

    public static boolean hasMasterwork(ItemStack stack) {
        CompoundTag mw = getMasterworkTag(stack);
        return mw != null && !mw.isEmpty() && mw.contains(VALUE_KEY, Tag.TAG_DOUBLE);
    }

    @Nullable
    public static CompoundTag getMasterworkTag(ItemStack stack) {
        if (!stack.hasTag()) return null;
        CompoundTag root = stack.getTagElement(OtherworldOrigins.MODID);
        if (root == null) return null;
        if (!root.contains(MOD_SUBROOT)) return null;
        return root.getCompound(MOD_SUBROOT).getCompound(MASTERWORK_KEY);
    }

    public static void clearMasterwork(ItemStack stack) {
        if (!stack.hasTag()) return;
        CompoundTag tag = stack.getTag();
        if (!tag.contains(OtherworldOrigins.MODID, Tag.TAG_COMPOUND)) return;
        CompoundTag root = tag.getCompound(OtherworldOrigins.MODID);
        if (!root.contains(MOD_SUBROOT, Tag.TAG_COMPOUND)) return;
        CompoundTag aff = root.getCompound(MOD_SUBROOT);
        aff.remove(MASTERWORK_KEY);
        if (aff.isEmpty()) {
            root.remove(MOD_SUBROOT);
        }
        if (root.isEmpty()) {
            tag.remove(OtherworldOrigins.MODID);
        }
    }

    /**
     * Writes masterwork state. Creates or reuses the first UUID entry like Apotheosis attribute affixes.
     */
    public static void putMasterwork(ItemStack stack, Attribute attribute, AttributeModifier.Operation operation, double value) {
        CompoundTag root = stack.getOrCreateTagElement(OtherworldOrigins.MODID);
        CompoundTag aff = root.getCompound(MOD_SUBROOT);
        CompoundTag mw = aff.getCompound(MASTERWORK_KEY);

        List<UUID> existing = readUuidList(mw);
        if (existing.isEmpty()) {
            writeUuidList(mw, List.of(UUID.randomUUID()));
        }

        mw.putString(ATTRIBUTE_KEY, BuiltInRegistries.ATTRIBUTE.getKey(attribute).toString());
        mw.putString(OPERATION_KEY, operation.name());
        mw.putDouble(VALUE_KEY, value);

        aff.put(MASTERWORK_KEY, mw);
        root.put(MOD_SUBROOT, aff);
    }

    public static List<UUID> readUuidList(CompoundTag masterworkTag) {
        List<UUID> out = new ArrayList<>();
        if (!masterworkTag.contains(UUIDS_KEY, Tag.TAG_LIST)) {
            return out;
        }
        ListTag list = masterworkTag.getList(UUIDS_KEY, Tag.TAG_INT_ARRAY);
        for (Tag t : list) {
            out.add(NbtUtils.loadUUID(t));
        }
        return out;
    }

    private static void writeUuidList(CompoundTag masterworkTag, List<UUID> uuids) {
        ListTag list = new ListTag();
        for (UUID id : uuids) {
            list.add(NbtUtils.createUUID(id));
        }
        masterworkTag.put(UUIDS_KEY, list);
    }

    /** Same string pattern as {@code AttributeAffix}: {@code "affix:" + resourceLocation}. */
    public static String affixModifierName(ResourceLocation affixId) {
        return "affix:" + affixId;
    }

    @Nullable
    public static AttributeModifier readAsModifier(ItemStack stack) {
        CompoundTag mw = getMasterworkTag(stack);
        if (mw == null || !mw.contains(ATTRIBUTE_KEY) || !mw.contains(UUIDS_KEY)) {
            return null;
        }
        List<UUID> uuids = readUuidList(mw);
        if (uuids.isEmpty()) return null;
        Attribute attr = BuiltInRegistries.ATTRIBUTE.get(ResourceLocation.parse(mw.getString(ATTRIBUTE_KEY)));
        if (attr == null) return null;
        AttributeModifier.Operation op;
        try {
            op = AttributeModifier.Operation.valueOf(mw.getString(OPERATION_KEY));
        } catch (IllegalArgumentException e) {
            return null;
        }
        double value = mw.getDouble(VALUE_KEY);
        return new AttributeModifier(uuids.get(0), affixModifierName(MASTERWORK_AFFIX_ID), value, op);
    }

    @Nullable
    public static Attribute readAttribute(ItemStack stack) {
        CompoundTag mw = getMasterworkTag(stack);
        if (mw == null || !mw.contains(ATTRIBUTE_KEY)) return null;
        return BuiltInRegistries.ATTRIBUTE.get(ResourceLocation.parse(mw.getString(ATTRIBUTE_KEY)));
    }
}
