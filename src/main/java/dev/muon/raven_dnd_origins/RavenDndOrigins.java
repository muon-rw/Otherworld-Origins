package dev.muon.raven_dnd_origins;

import com.mojang.logging.LogUtils;
import dev.muon.raven_dnd_origins.action.ModActions;
import dev.muon.raven_dnd_origins.commands.SpellDump;
import dev.muon.raven_dnd_origins.component.ModDataComponents;
import dev.muon.raven_dnd_origins.effect.ModEffects;
import dev.muon.raven_dnd_origins.condition.ModConditions;
import dev.muon.raven_dnd_origins.config.RavenDndOriginsConfig;
import dev.muon.raven_dnd_origins.attribute.ModAttributes;
import dev.muon.raven_dnd_origins.capability.BrewerTrackerCapability;
import dev.muon.raven_dnd_origins.entity.ModEntities;
import dev.muon.raven_dnd_origins.item.ModCreativeTabs;
import dev.muon.raven_dnd_origins.item.ModItems;
import dev.muon.raven_dnd_origins.network.RavenDndOriginsNetwork;
import dev.muon.raven_dnd_origins.power.ModPowers;
import dev.muon.raven_dnd_origins.school.ModSchools;
import dev.muon.raven_dnd_origins.skills.ModPassives;
import dev.muon.raven_dnd_origins.skills.ModSkills;
import dev.muon.raven_dnd_origins.sound.ModSounds;
import dev.muon.raven_dnd_origins.spells.ModSpells;
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.api.RegisterType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Mod(RavenDndOrigins.MODID)
public class RavenDndOrigins {
    public static final String MODID = "raven_dnd_origins";

    public static ResourceLocation loc(String id) {
        return ResourceLocation.fromNamespaceAndPath(MODID, id);
    }

    public static final Logger LOGGER = LogUtils.getLogger();

    public RavenDndOrigins(IEventBus modBus, ModContainer container) {

        RavenDndOrigins.LOGGER.info("Loading Raven DnD Origins");
        deleteLegacyForgeConfig();
        RavenDndOriginsConfig.setInstance(
                ConfigApiJava.registerAndLoadConfig(RavenDndOriginsConfig::new, RegisterType.BOTH));

        ModAttributes.register(modBus);
        ModEntities.register(modBus);
        ModEffects.register(modBus);
        ModItems.register(modBus);
        ModCreativeTabs.register(modBus);
        ModSounds.register(modBus);
        ModDataComponents.register(modBus);

        ModActions.register();
        ModConditions.register();
        ModPowers.register();

        ModSchools.register(modBus);
        ModSpells.register(modBus);

        ModPassives.register(modBus);
        ModSkills.register(modBus);

        BrewerTrackerCapability.register(modBus);

        modBus.addListener(RavenDndOriginsNetwork::register);


        if ("1".equals(System.getenv("RAVEN_DND_ORIGINS_DUMP_SPELLS"))) {
            NeoForge.EVENT_BUS.addListener(SpellDump::onServerStarted);
        }
    }

    private static void deleteLegacyForgeConfig() {
        Path legacy = FMLPaths.CONFIGDIR.get().resolve("raven_dnd_origins-common.toml");
        try {
            if (Files.deleteIfExists(legacy)) {
                LOGGER.info("Deleted legacy Forge config {} (replaced by FzzyConfig in 2.0.0)", legacy);
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to delete legacy Forge config {}: {}", legacy, e.toString());
        }
    }
}
