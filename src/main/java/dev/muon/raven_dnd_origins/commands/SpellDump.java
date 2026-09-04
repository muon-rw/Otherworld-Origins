package dev.muon.raven_dnd_origins.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.restrictions.ModSpellTags;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SpellDump {

    private static final Map<String, TagKey<AbstractSpell>> CATEGORY_TAGS = new LinkedHashMap<>();

    static {
        CATEGORY_TAGS.put("conjuring", ModSpellTags.CONJURING);
        CATEGORY_TAGS.put("control", ModSpellTags.CONTROL);
        CATEGORY_TAGS.put("defensive", ModSpellTags.DEFENSIVE);
        CATEGORY_TAGS.put("melee", ModSpellTags.MELEE);
        CATEGORY_TAGS.put("offensive", ModSpellTags.OFFENSIVE);
        CATEGORY_TAGS.put("support", ModSpellTags.SUPPORT);
        CATEGORY_TAGS.put("utility", ModSpellTags.UTILITY);
        CATEGORY_TAGS.put("unrestricted", ModSpellTags.UNRESTRICTED);
        CATEGORY_TAGS.put("wild_magic_surge", ModSpellTags.WILD_MAGIC_SURGE);
    }

    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        int count = write(server);
        RavenDndOrigins.LOGGER.info("Dev spell dump wrote {} spells; halting server", count);
        server.halt(false);
    }

    public static int write(MinecraftServer server) {
        List<SpellData> spellDataList = new ArrayList<>();

        for (AbstractSpell spell : SpellRegistry.REGISTRY) {
            if (spell == SpellRegistry.none()) {
                continue;
            }

            Holder<AbstractSpell> holder = SpellRegistry.REGISTRY.wrapAsHolder(spell);
            List<String> tags = new ArrayList<>();
            for (Map.Entry<String, TagKey<AbstractSpell>> entry : CATEGORY_TAGS.entrySet()) {
                SpellRegistry.REGISTRY.getTag(entry.getValue())
                        .filter(named -> named.contains(holder))
                        .ifPresent(named -> tags.add(entry.getKey()));
            }

            // SpellConfigManager only builds its map on the first OnDatapackSyncEvent, so the config-backed
            // getters (getSchoolType, getMaxLevel, getRarity, getSpellCooldown) return parameter defaults
            // until a player joins. The per-spell DefaultConfig is the static truth and is always populated.
            ResourceLocation spellResource = spell.getSpellResource();
            DefaultConfig defaults = spell.getDefaultConfig();
            SchoolType school = SchoolRegistry.getSchool(defaults.schoolResource);
            int minLevel = spell.getMinLevel();
            int maxLevel = defaults.maxLevel;
            SpellRarity[] rarities = SpellRarity.values();
            SpellRarity maxRarity = rarities[Math.min(defaults.minRarity.ordinal() + maxLevel - minLevel, rarities.length - 1)];
            String name = Component.translatable(spell.getComponentId()).getString();
            String description = Component.translatable(spell.getComponentId() + ".guide").getString();

            spellDataList.add(new SpellData(
                    spell.getSpellId(),
                    spellResource.getNamespace(),
                    name,
                    defaults.schoolResource.toString(),
                    school == null ? defaults.schoolResource.getPath() : school.getDisplayName().getString(),
                    minLevel,
                    maxLevel,
                    defaults.minRarity.name(),
                    maxRarity.name(),
                    spell.getCastType().name(),
                    spell.getCastTime(minLevel),
                    spell.getManaCost(minLevel),
                    spell.getManaCost(maxLevel),
                    defaults.cooldownInSeconds,
                    description,
                    tags
            ));
        }

        try {
            Path modConfigDir = FMLPaths.CONFIGDIR.get().resolve(RavenDndOrigins.MODID);
            Files.createDirectories(modConfigDir);

            Path outputFile = modConfigDir.resolve("spells.json");

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(outputFile, gson.toJson(spellDataList));

            RavenDndOrigins.LOGGER.info("Spell dump written to: {}", outputFile);
            return spellDataList.size();
        } catch (IOException e) {
            RavenDndOrigins.LOGGER.error("Failed to write spell dump to file", e);
            return 0;
        }
    }

    private record SpellData(String id, String mod, String name, String school, String schoolName,
                              int minLevel, int maxLevel, String rarityAtMinLevel, String rarityAtMaxLevel,
                              String castType, int castTime, int manaCostAtMinLevel, int manaCostAtMaxLevel,
                              double cooldownSeconds, String description, List<String> tags) {
    }
}
