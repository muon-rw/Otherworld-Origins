package dev.muon.otherworldorigins.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.restrictions.ModSpellTags;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import io.github.edwinmindcraft.origins.common.OriginsCommon;
import io.github.edwinmindcraft.origins.common.network.S2COpenOriginScreen;
import io.github.edwinmindcraft.origins.common.registry.OriginRegisters;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.tags.ITagManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mod.EventBusSubscriber(modid = OtherworldOrigins.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModCommands {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("otherworldorigins")
                .then(Commands.literal("gui")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> openGui(context.getSource(), List.of(context.getSource().getPlayerOrException())))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> openGui(context.getSource(), EntityArgument.getPlayers(context, "targets").stream().toList()))
                        )
                )
                .then(Commands.literal("dumpSpells")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> dumpSpells(context.getSource()))
                )
        );
    }

    private static int openGui(CommandSourceStack source, List<ServerPlayer> targets) {
        for (ServerPlayer target : targets) {
            IOriginContainer.get(target).ifPresent(container -> {
                List<Holder.Reference<OriginLayer>> activeLayers = OriginsAPI.getActiveLayers().stream()
                        .sorted(Comparator.comparing(Holder::value))
                        .toList();
                for (Holder<OriginLayer> layer : activeLayers) {
                    container.setOrigin(layer.unwrapKey().orElseThrow(), OriginRegisters.EMPTY.getKey());
                }
                container.synchronize();
                container.checkAutoChoosingLayers(false);
                OriginsCommon.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> target),
                        new S2COpenOriginScreen(false)
                );
            });
        }
        if (targets.size() == 1) {
            source.sendSuccess(() -> Component.literal("Opened origin selection for " + targets.get(0).getDisplayName().getString()), true);
        } else {
            source.sendSuccess(() -> Component.literal("Opened origin selection for " + targets.size() + " players"), true);
        }
        return targets.size();
    }

    private static int dumpSpells(CommandSourceStack source) {
        var spells = SpellRegistry.REGISTRY.get().getValues();
        ITagManager<AbstractSpell> tagManager = SpellRegistry.REGISTRY.get().tags();
        List<SpellData> spellDataList = new ArrayList<>();

        for (AbstractSpell spell : spells) {
            if (spell == SpellRegistry.none()) {
                continue;
            }

            String spellId = spell.getSpellId();

            String categoryString = "UNKNOWN";
            if (tagManager != null) {
                List<TagKey<AbstractSpell>> categoryTags = List.of(
                        ModSpellTags.OFFENSIVE, ModSpellTags.SUPPORT, ModSpellTags.DEFENSIVE
                );
                categoryString = categoryTags.stream()
                        .filter(tag -> tagManager.getTag(tag).contains(spell))
                        .map(tag -> tag.location().getPath().toUpperCase())
                        .collect(Collectors.joining(","));
                if (categoryString.isEmpty()) {
                    categoryString = "UNCATEGORIZED";
                }
            }

            SchoolType school = spell.getSchoolType();
            String schoolName = school.getId().toString();
            int minRarityInt = spell.getMinRarity();
            SpellRarity minRarity = SpellRarity.values()[minRarityInt];

            CastType castType = spell.getCastType();
            String description = Component.translatable(spell.getComponentId() + ".guide").getString();

            spellDataList.add(new SpellData(spellId, categoryString, schoolName, minRarity.name(), castType.name(), description));
        }

        try {
            Path configDir = FMLPaths.CONFIGDIR.get();
            Path modConfigDir = configDir.resolve(OtherworldOrigins.MODID);
            Files.createDirectories(modConfigDir);

            Path outputFile = modConfigDir.resolve("spells.json");

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(spellDataList);

            Files.writeString(outputFile, json);

            source.sendSuccess(() -> Component.literal("Spell dump complete! Written to: " + outputFile), true);
            OtherworldOrigins.LOGGER.info("Spell dump written to: {}", outputFile);

            return spellDataList.size();
        } catch (IOException e) {
            OtherworldOrigins.LOGGER.error("Failed to write spell dump to file", e);
            source.sendFailure(Component.literal("Failed to write spell dump: " + e.getMessage()));
            return 0;
        }
    }

    private static class SpellData {
        private final String spellId;
        private final String category;
        private final String school;
        private final String minRarity;
        private final String castType;
        private final String description;

        public SpellData(String spellId, String category, String school, String minRarity, String castType, String description) {
            this.spellId = spellId;
            this.category = category;
            this.school = school;
            this.minRarity = minRarity;
            this.castType = castType;
            this.description = description;
        }

        public String getSpellId() {
            return spellId;
        }

        public String getCategory() {
            return category;
        }

        public String getSchool() {
            return school;
        }

        public String getMinRarity() {
            return minRarity;
        }

        public String getCastType() {
            return castType;
        }

        public String getDescription() {
            return description;
        }
    }
}
