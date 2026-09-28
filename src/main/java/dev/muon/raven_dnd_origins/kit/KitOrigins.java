package dev.muon.raven_dnd_origins.kit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.muon.raven_dnd_origins.selection.SelectionSessions;
import dev.overgrown.origins.component.PlayerOriginsAttachment;
import dev.overgrown.origins.component.PlayerOriginsImpl;
import dev.overgrown.origins.network.OriginsServerNetwork;
import dev.overgrown.origins.origin.OriginLayer;
import dev.overgrown.origins.origin.OriginLayers;
import dev.overgrown.origins.origin.OriginManager;
import dev.overgrown.origins.origin.OriginRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

final class KitOrigins {
    private KitOrigins() {}

    static void clear(ServerPlayer player) {
        OriginManager.clearAllLayers(player);
    }

    static void choose(ServerPlayer player, JsonObject origins, List<String> problems) {
        Map<OriginLayer, ResourceLocation> choices = new TreeMap<>();
        for (Map.Entry<String, JsonElement> entry : origins.entrySet()) {
            OriginLayer layer = OriginLayers.get(ResourceLocation.parse(entry.getKey()));
            ResourceLocation origin = ResourceLocation.parse(entry.getValue().getAsString());
            if (layer == null) {
                problems.add("unknown layer " + entry.getKey());
            } else if (OriginRegistry.get(origin) == null) {
                problems.add("unknown origin " + origin);
            } else {
                choices.put(layer, origin);
            }
        }
        // Layer order, so level- and class-gated layers see their prerequisites already chosen
        choices.forEach((layer, origin) -> {
            OriginManager.chooseOrigin(player, layer.id(), origin, false);
            if (!origin.equals(chosen(player, layer))) {
                problems.add(layer.id() + " rejected " + origin + " (its conditions decide it)");
            }
        });
        OriginManager.checkAutoChoosingLayers(player, true);
        OriginsServerNetwork.broadcastPlayerOrigins(player.getServer(), player);
        reportUnchosen(player, problems);
        SelectionSessions.clear(player);
    }

    static JsonObject capture(ServerPlayer player) {
        JsonObject origins = new JsonObject();
        for (OriginLayer layer : OriginLayers.enabledOrdered()) {
            ResourceLocation origin = chosen(player, layer);
            if (!OriginRegistry.EMPTY_ID.equals(origin)) {
                origins.addProperty(layer.id().toString(), origin.toString());
            }
        }
        return origins;
    }

    private static ResourceLocation chosen(ServerPlayer player, OriginLayer layer) {
        PlayerOriginsImpl state = PlayerOriginsAttachment.get(player);
        return state == null ? OriginRegistry.EMPTY_ID : state.getOrigin(layer.id());
    }

    private static void reportUnchosen(ServerPlayer player, List<String> problems) {
        for (OriginLayer layer : OriginLayers.enabledFor(player)) {
            if (!layer.swappable() && OriginRegistry.EMPTY_ID.equals(chosen(player, layer)) && OriginManager.hasChoosableOrigins(player, layer)) {
                problems.add("no choice for layer " + layer.id());
            }
        }
    }
}
