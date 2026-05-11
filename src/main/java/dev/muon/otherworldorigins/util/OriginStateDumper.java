package dev.muon.otherworldorigins.util;

import dev.muon.otherworldorigins.OtherworldOrigins;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Writes Origins/Apoli registry sizes and per-player container contents to a file in
 * {@code <gamedir>/logs/} and logs a one-line summary. Designed for cross-side diagnosis
 * of the validation-loop disconnect: when the client kicks itself after
 * {@code MAX_VALIDATION_ATTEMPTS}, both sides dump in parallel so we can compare
 * registry counts and container contents and see exactly which sync is incomplete.
 *
 * <p>Pass {@code server} as the player's {@link MinecraftServer} when called server-side
 * and {@code null} when called client-side — the API resolves to the appropriate Calio
 * dynamic registry view either way.</p>
 */
public final class OriginStateDumper {
    private OriginStateDumper() {}

    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public static void dump(Player player, String side, MinecraftServer server, String reason) {
        if (player == null) {
            OtherworldOrigins.LOGGER.warn("[StateDump:{}] Skipping dump: null player ({})", side, reason);
            return;
        }

        Registry<OriginLayer> layers = OriginsAPI.getLayersRegistry(server);
        Registry<Origin> origins = OriginsAPI.getOriginsRegistry(server);
        Registry<ConfiguredPower<?, ?>> powers = ApoliAPI.getPowers(server);

        IOriginContainer originContainer = IOriginContainer.get(player).resolve().orElse(null);
        IPowerContainer powerContainer = ApoliAPI.getPowerContainer(player);

        int layerCount = layers == null ? -1 : layers.size();
        int originCount = origins == null ? -1 : origins.size();
        int powerCount = powers == null ? -1 : powers.size();
        int containerLayerCount = originContainer == null ? -1 : countContainerLayers(originContainer, layers);
        int containerPowerCount = powerContainer == null ? -1 : powerContainer.getPowerTypes(true).size();

        OtherworldOrigins.LOGGER.warn(
                "[StateDump:{}] {} | player={} uuid={} | reg layers={} origins={} powers={} | container layers={} powers={}",
                side, reason, player.getName().getString(), player.getUUID(),
                layerCount, originCount, powerCount,
                containerLayerCount, containerPowerCount);

        Path file = resolveDumpFile(side, player);
        try {
            Files.createDirectories(file.getParent());
            try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(file))) {
                writeDump(w, player, side, reason, layers, origins, powers, originContainer, powerContainer);
            }
            OtherworldOrigins.LOGGER.warn("[StateDump:{}] Wrote {}", side, file);
        } catch (IOException e) {
            OtherworldOrigins.LOGGER.error("[StateDump:{}] Failed to write {}: {}", side, file, e.toString());
        }
    }

    private static int countContainerLayers(IOriginContainer container, Registry<OriginLayer> layers) {
        if (layers == null) return -1;
        int n = 0;
        for (OriginLayer layer : layers) {
            ResourceKey<OriginLayer> layerKey = layers.getResourceKey(layer).orElse(null);
            if (layerKey == null) continue;
            Holder<OriginLayer> holder = layers.getHolderOrThrow(layerKey);
            if (container.hasOrigin(holder)) n++;
        }
        return n;
    }

    private static Path resolveDumpFile(String side, Player player) {
        String ts = LocalDateTime.now().format(FILE_TS);
        String filename = String.format("ow-origins-statedump-%s-%s-%s.txt",
                side.toLowerCase(), ts, player.getUUID());
        return FMLPaths.GAMEDIR.get().resolve("logs").resolve(filename);
    }

    private static void writeDump(PrintWriter w, Player player, String side, String reason,
                                  Registry<OriginLayer> layers,
                                  Registry<Origin> origins,
                                  Registry<ConfiguredPower<?, ?>> powers,
                                  IOriginContainer originContainer,
                                  IPowerContainer powerContainer) {
        w.println("=== Otherworld Origins State Dump ===");
        w.println("Side:       " + side);
        w.println("Reason:     " + reason);
        w.println("Player:     " + player.getName().getString());
        w.println("UUID:       " + player.getUUID());
        w.println("Dimension:  " + player.level().dimension().location());
        w.println("Timestamp:  " + LocalDateTime.now());
        w.println();

        w.println("[Origin Layer Registry]");
        if (layers == null) {
            w.println("  (null registry)");
        } else {
            w.println("  Count: " + layers.size());
            List<ResourceLocation> ids = layers.keySet().stream()
                    .sorted(Comparator.comparing(ResourceLocation::toString))
                    .toList();
            for (ResourceLocation id : ids) {
                OriginLayer layer = layers.get(id);
                String enabled = (layer != null && layer.enabled()) ? "" : "  (disabled)";
                int avail = (layer != null) ? layer.origins(player).size() : -1;
                w.println("  - " + id + "  available_for_player=" + avail + enabled);
            }
        }
        w.println();

        w.println("[Origin Registry]");
        if (origins == null) {
            w.println("  (null registry)");
        } else {
            w.println("  Count: " + origins.size());
            origins.keySet().stream()
                    .sorted(Comparator.comparing(ResourceLocation::toString))
                    .forEach(id -> w.println("  - " + id));
        }
        w.println();

        w.println("[Power Registry]");
        if (powers == null) {
            w.println("  (null registry)");
        } else {
            w.println("  Count: " + powers.size());
            powers.keySet().stream()
                    .sorted(Comparator.comparing(ResourceLocation::toString))
                    .forEach(id -> w.println("  - " + id));
        }
        w.println();

        w.println("[Player Origin Container]");
        if (originContainer == null) {
            w.println("  (null container)");
        } else if (layers == null) {
            w.println("  (cannot enumerate: null layer registry)");
        } else {
            int picked = 0;
            for (OriginLayer layer : layers) {
                ResourceKey<OriginLayer> layerKey = layers.getResourceKey(layer).orElse(null);
                if (layerKey == null) continue;
                Holder<OriginLayer> holder = layers.getHolderOrThrow(layerKey);
                ResourceKey<Origin> originKey = originContainer.getOrigin(holder);
                if (originKey != null) {
                    w.println("  - " + layerKey.location() + "  ->  " + originKey.location());
                    if (!originKey.location().equals(ResourceLocation.fromNamespaceAndPath("origins", "empty"))) {
                        picked++;
                    }
                }
            }
            w.println("  Total non-empty selections: " + picked);
        }
        w.println();

        w.println("[Player Power Container]");
        if (powerContainer == null) {
            w.println("  (null container)");
        } else {
            Set<ResourceKey<ConfiguredPower<?, ?>>> types = powerContainer.getPowerTypes(true);
            w.println("  Count: " + types.size());
            types.stream()
                    .sorted(Comparator.comparing(k -> k.location().toString()))
                    .forEach(k -> {
                        Collection<ResourceLocation> sources = powerContainer.getSources(k);
                        w.println("  - " + k.location() + "  sources=" + sources);
                    });
        }
        w.println();

        w.println("=== End ===");
    }
}
