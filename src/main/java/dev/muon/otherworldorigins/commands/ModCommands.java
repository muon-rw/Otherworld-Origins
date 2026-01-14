package dev.muon.otherworldorigins.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.restrictions.SpellCategory;
import dev.muon.otherworldorigins.restrictions.SpellCategoryMapper;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = OtherworldOrigins.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModCommands {
    
    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        dispatcher.register(Commands.literal("otherworldorigins")
                .then(Commands.literal("dumpSpells")
                        .requires(source -> source.hasPermission(2)) // Requires OP level 2
                        .executes(context -> dumpSpells(context.getSource()))
                )
        );
    }
    
    private static int dumpSpells(CommandSourceStack source) {
        // Ensure SpellCategoryMapper is initialized
        SpellCategoryMapper.initialize();
        
        var spells = SpellRegistry.REGISTRY.get().getValues();
        List<SpellData> spellDataList = new ArrayList<>();
        
        for (AbstractSpell spell : spells) {
            if (spell == SpellRegistry.none()) {
                continue; // Skip the "none" spell
            }
            
            String spellId = spell.getSpellId();
            Set<SpellCategory> categories = SpellCategoryMapper.getCategories(spell);
            String categoryString = categories.stream()
                    .map(Enum::name)
                    .reduce((a, b) -> a + "," + b)
                    .orElse("UNKNOWN");
            SchoolType school = spell.getSchoolType();
            String schoolName = school.getId().toString();
            int minRarityInt = spell.getMinRarity();
            SpellRarity minRarity = SpellRarity.values()[minRarityInt];
            
            spellDataList.add(new SpellData(spellId, categoryString, schoolName, minRarity.name()));
        }
        
        // Write to JSON file
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
        
        public SpellData(String spellId, String category, String school, String minRarity) {
            this.spellId = spellId;
            this.category = category;
            this.school = school;
            this.minRarity = minRarity;
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
    }
}
