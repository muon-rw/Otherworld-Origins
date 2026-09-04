package dev.muon.raven_dnd_origins.util;
import dev.muon.raven_dnd_origins.RavenDndOrigins;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class TagRefs {
    public static final TagKey<Item> MARTIAL_FOCUS = ItemTags.create(RavenDndOrigins.loc("martial_focus"));
    public static final TagKey<Item> ARCHERY_FOCUS = ItemTags.create(RavenDndOrigins.loc("archery_focus"));
}