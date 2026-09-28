package dev.muon.raven_dnd_origins.kit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.muon.raven_dnd_origins.RavenDndOrigins;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

final class DebugKits {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String EXTENSION = ".json";

    private DebugKits() {}

    static Path directory() {
        return FMLPaths.CONFIGDIR.get().resolve(RavenDndOrigins.MODID).resolve("kits");
    }

    static List<String> names() {
        Path dir = directory();
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.map(file -> file.getFileName().toString())
                    .filter(name -> name.endsWith(EXTENSION))
                    .map(name -> name.substring(0, name.length() - EXTENSION.length()))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            RavenDndOrigins.LOGGER.warn("Could not list debug kits in {}", dir, e);
            return List.of();
        }
    }

    static JsonObject read(String name) throws IOException {
        try (Reader reader = Files.newBufferedReader(file(name), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    static Path write(String name, JsonObject kit) throws IOException {
        Path file = file(name);
        Files.createDirectories(file.getParent());
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(kit, writer);
        }
        return file;
    }

    private static Path file(String name) {
        return directory().resolve(name + EXTENSION);
    }
}
