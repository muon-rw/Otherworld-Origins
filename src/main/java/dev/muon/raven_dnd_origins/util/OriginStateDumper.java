package dev.muon.raven_dnd_origins.util;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.origins.component.PlayerOriginsAttachment;
import dev.overgrown.origins.component.PlayerOriginsImpl;
import dev.overgrown.origins.origin.OriginLayer;
import dev.overgrown.origins.origin.OriginLayers;
import dev.overgrown.origins.origin.OriginRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Writes Origins/Apoli registry sizes and per-player container contents to a file in
 * {@code <gamedir>/logs/} and logs a one-line summary, so a suspected client/server desync can be
 * diagnosed by dumping both sides in parallel and comparing them.
 *
 * <p>{@code server} is accepted for call-site compatibility and is unused: Overgrown's origin,
 * layer and power tables are static and side-local.</p>
 */
public final class OriginStateDumper {
    private OriginStateDumper() {}

    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public static void dump(Player player, String side, MinecraftServer server, String reason) {
        if (player == null) {
            RavenDndOrigins.LOGGER.warn("[StateDump:{}] Skipping dump: null player ({})", side, reason);
            return;
        }

        PlayerOriginsImpl origins = PlayerOriginsAttachment.get(player);
        PowerContainer powers = PowerContainer.of(player);

        RavenDndOrigins.LOGGER.warn(
                "[StateDump:{}] {} | player={} uuid={} | reg layers={} origins={} powers={} | container layers={} powers={}",
                side, reason, player.getName().getString(), player.getUUID(),
                OriginLayers.size(), OriginRegistry.size(), ApoliPowers.view().size(),
                origins == null ? -1 : origins.snapshot().size(),
                powers == null ? -1 : powers.allPowers().size());

        Path file = resolveDumpFile(side, player);
        try {
            Files.createDirectories(file.getParent());
            try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(file))) {
                writeDump(w, player, side, reason, origins, powers);
            }
            RavenDndOrigins.LOGGER.warn("[StateDump:{}] Wrote {}", side, file);
        } catch (IOException e) {
            RavenDndOrigins.LOGGER.error("[StateDump:{}] Failed to write {}: {}", side, file, e.toString());
        }
    }

    private static Path resolveDumpFile(String side, Player player) {
        String ts = LocalDateTime.now().format(FILE_TS);
        String filename = String.format("raven-dnd-origins-statedump-%s-%s-%s.txt",
                side.toLowerCase(), ts, player.getUUID());
        return FMLPaths.GAMEDIR.get().resolve("logs").resolve(filename);
    }

    private static void writeDump(PrintWriter w, Player player, String side, String reason,
                                  PlayerOriginsImpl origins, PowerContainer powers) {
        w.println("=== Raven DnD Origins State Dump ===");
        w.println("Side:       " + side);
        w.println("Reason:     " + reason);
        w.println("Player:     " + player.getName().getString());
        w.println("UUID:       " + player.getUUID());
        w.println("Dimension:  " + player.level().dimension().location());
        w.println("Timestamp:  " + LocalDateTime.now());
        w.println();

        w.println("[Origin Layer Registry]");
        w.println("  Count: " + OriginLayers.size());
        List<OriginLayer> layers = OriginLayers.all().stream()
                .sorted(Comparator.comparing(layer -> layer.id().toString()))
                .toList();
        for (OriginLayer layer : layers) {
            String enabled = layer.enabled() ? "" : "  (disabled)";
            w.println("  - " + layer.id() + "  available_for_player=" + layer.availableOrigins(player).size() + enabled);
        }
        w.println();

        w.println("[Origin Registry]");
        w.println("  Count: " + OriginRegistry.size());
        OriginRegistry.all().stream()
                .map(origin -> origin.id().toString())
                .sorted()
                .forEach(id -> w.println("  - " + id));
        w.println();

        w.println("[Power Registry]");
        w.println("  Count: " + ApoliPowers.view().size());
        ApoliPowers.view().keySet().stream()
                .map(ResourceLocation::toString)
                .sorted()
                .forEach(id -> w.println("  - " + id));
        w.println();

        w.println("[Player Origins]");
        if (origins == null) {
            w.println("  (no attachment)");
        } else {
            Map<ResourceLocation, ResourceLocation> picks = origins.snapshot();
            int picked = 0;
            for (ResourceLocation layerId : picks.keySet().stream()
                    .sorted(Comparator.comparing(ResourceLocation::toString)).toList()) {
                ResourceLocation originId = picks.get(layerId);
                w.println("  - " + layerId + "  ->  " + originId);
                if (!OriginRegistry.EMPTY_ID.equals(originId)) {
                    picked++;
                }
            }
            w.println("  Total non-empty selections: " + picked);
            w.println("  Selecting origin: " + origins.isSelectingOrigin());
            w.println("  Swap pool: " + origins.poolSnapshot());
        }
        w.println();

        w.println("[Player Power Container]");
        if (powers == null) {
            w.println("  (null container)");
        } else {
            w.println("  Count: " + powers.allPowers().size());
            powers.allPowers().stream()
                    .sorted(Comparator.comparing(ResourceLocation::toString))
                    .forEach(id -> w.println("  - " + id + "  sources=" + powers.sourcesOf(id)
                            + (powers.isSuppressed(id) ? "  (suppressed)" : "")));
        }
        w.println();

        w.println("=== End ===");
    }
}
