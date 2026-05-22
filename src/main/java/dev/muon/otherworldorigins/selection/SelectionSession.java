package dev.muon.otherworldorigins.selection;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * A pending origin-layer selection for one player: which layers still need a choice, and why the
 * prompt was raised. Held server-side (persisted across disconnect/restart) and synced to the
 * client so the screen opens in the right mode.
 */
public record SelectionSession(List<ResourceLocation> layers, SessionKind kind) {

    public SelectionSession {
        layers = List.copyOf(layers);
    }

    public boolean isEmpty() {
        return layers.isEmpty();
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        ListTag layerList = new ListTag();
        for (ResourceLocation layer : layers) {
            layerList.add(StringTag.valueOf(layer.toString()));
        }
        tag.put("layers", layerList);
        tag.putString("kind", kind.name());
        return tag;
    }

    public static SelectionSession fromNbt(CompoundTag tag) {
        List<ResourceLocation> layers = new ArrayList<>();
        ListTag layerList = tag.getList("layers", Tag.TAG_STRING);
        for (int i = 0; i < layerList.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(layerList.getString(i));
            if (id != null) {
                layers.add(id);
            }
        }
        SessionKind kind;
        try {
            kind = SessionKind.valueOf(tag.getString("kind"));
        } catch (IllegalArgumentException unknownKind) {
            // A kind removed in a later version: fall back to the safest (full) flow.
            kind = SessionKind.INITIAL_CREATION;
        }
        return new SelectionSession(layers, kind);
    }

    public void toBuf(FriendlyByteBuf buf) {
        buf.writeCollection(layers, FriendlyByteBuf::writeResourceLocation);
        buf.writeEnum(kind);
    }

    public static SelectionSession fromBuf(FriendlyByteBuf buf) {
        List<ResourceLocation> layers = buf.readList(FriendlyByteBuf::readResourceLocation);
        SessionKind kind = buf.readEnum(SessionKind.class);
        return new SelectionSession(layers, kind);
    }
}
