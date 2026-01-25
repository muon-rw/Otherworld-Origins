package dev.muon.otherworldorigins.util;
import dev.muon.otherworldorigins.OtherworldOrigins;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class TagRefs {
    public static final TagKey<Item> MARTIAL_FOCUS = ItemTags.create(OtherworldOrigins.loc("martial_focus"));
    public static final TagKey<Item> ARCHERY_FOCUS = ItemTags.create(OtherworldOrigins.loc("archery_focus"));
}