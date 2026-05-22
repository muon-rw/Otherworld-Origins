package dev.muon.otherworldorigins;

import com.mojang.logging.LogUtils;
import dev.muon.otherworldorigins.action.ModActions;
import dev.muon.otherworldorigins.effect.ModEffects;
import dev.muon.otherworldorigins.condition.ModConditions;
import dev.muon.otherworldorigins.config.OtherworldOriginsConfig;
import dev.muon.otherworldorigins.attribute.ModAttributes;
import dev.muon.otherworldorigins.capability.BrewerTrackerCapability;
import dev.muon.otherworldorigins.entity.ModEntities;
import dev.muon.otherworldorigins.item.ModItems;
import dev.muon.otherworldorigins.network.*;
import dev.muon.otherworldorigins.power.ModPowers;
import dev.muon.otherworldorigins.school.ModSchools;
import dev.muon.otherworldorigins.skills.ModPassives;
import dev.muon.otherworldorigins.skills.ModSkills;
import dev.muon.otherworldorigins.sound.ModSounds;
import dev.muon.otherworldorigins.spells.ModSpells;
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.api.RegisterType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Mod(OtherworldOrigins.MODID)
public class OtherworldOrigins {
    public static final String MODID = "otherworldorigins";

    /**
     * Shared goblin / hobgoblin power: Goblin's Tyranny disguise effect + cosmetic ears layer.
     */
    public static final ResourceLocation GOBLINS_TYRANNY_KIN_POWER = loc("subrace/other/goblin/goblins_tyranny_kin");

    public static ResourceLocation loc(String id) {
        return ResourceLocation.fromNamespaceAndPath(MODID, id);
    }

    public static final Logger LOGGER = LogUtils.getLogger();

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            loc("main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public OtherworldOrigins(FMLJavaModLoadingContext context) {

        OtherworldOrigins.LOGGER.info("Loading Otherworld Origins");
        deleteLegacyForgeConfig();
        OtherworldOriginsConfig.setInstance(
                ConfigApiJava.registerAndLoadConfig(OtherworldOriginsConfig::new, RegisterType.BOTH));

        IEventBus modEventBus = context.getModEventBus();
        modEventBus.addListener(this::commonSetup);

        ModAttributes.register(modEventBus);
        ModEntities.register(modEventBus);
        ModEffects.register(modEventBus);
        ModItems.register(modEventBus);
        ModSounds.register(modEventBus);

        ModActions.register(modEventBus);
        ModConditions.register(modEventBus);
        ModPowers.register(modEventBus);

        ModSchools.register(modEventBus);
        ModSpells.register(modEventBus);

        ModPassives.register(modEventBus);
        ModSkills.register(modEventBus);

        BrewerTrackerCapability.register(modEventBus, MinecraftForge.EVENT_BUS);

        MinecraftForge.EVENT_BUS.register(this);

        registerMessages();
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }

    private static void deleteLegacyForgeConfig() {
        Path legacy = FMLPaths.CONFIGDIR.get().resolve("otherworldorigins-common.toml");
        try {
            if (Files.deleteIfExists(legacy)) {
                LOGGER.info("Deleted legacy Forge config {} (replaced by FzzyConfig in 2.0.0)", legacy);
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to delete legacy Forge config {}: {}", legacy, e.toString());
        }
    }

    private static int packetId = 0;

    private static int nextPacketId() {
        return packetId++;
    }

    public static void registerMessages() {
        CHANNEL.messageBuilder(CloseCurrentScreenMessage.class, nextPacketId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder((msg, buf) -> {})
                .decoder(buf -> new CloseCurrentScreenMessage())
                .consumerMainThread(CloseCurrentScreenMessage::handle)
                .add();

        CHANNEL.messageBuilder(ResetOriginsMessage.class, nextPacketId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(ResetOriginsMessage::encode)
                .decoder(ResetOriginsMessage::decode)
                .consumerMainThread(ResetOriginsMessage::handle)
                .add();

        CHANNEL.messageBuilder(C2SRevertLayerOriginsMessage.class, nextPacketId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SRevertLayerOriginsMessage::encode)
                .decoder(C2SRevertLayerOriginsMessage::decode)
                .consumerMainThread(C2SRevertLayerOriginsMessage::handle)
                .add();

        CHANNEL.messageBuilder(RespecAptitudesMessage.class, nextPacketId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(RespecAptitudesMessage::encode)
                .decoder(RespecAptitudesMessage::decode)
                .consumerMainThread(RespecAptitudesMessage::handle)
                .add();
        CHANNEL.messageBuilder(OpenFinalConfirmScreenMessage.class, nextPacketId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenFinalConfirmScreenMessage::encode)
                .decoder(OpenFinalConfirmScreenMessage::decode)
                .consumerMainThread(OpenFinalConfirmScreenMessage::handle)
                .add();
        
        CHANNEL.messageBuilder(GiveStarterKitMessage.class, nextPacketId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(GiveStarterKitMessage::encode)
                .decoder(GiveStarterKitMessage::decode)
                .consumerMainThread(GiveStarterKitMessage::handle)
                .add();
        CHANNEL.messageBuilder(PlayPlayerAnimationPacket.class, nextPacketId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PlayPlayerAnimationPacket::encode)
                .decoder(PlayPlayerAnimationPacket::decode)
                .consumerMainThread(PlayPlayerAnimationPacket::handle)
                .add();
        CHANNEL.messageBuilder(ShapeshiftSyncMessage.class, nextPacketId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ShapeshiftSyncMessage::encode)
                .decoder(ShapeshiftSyncMessage::decode)
                .consumerMainThread(ShapeshiftSyncMessage::handle)
                .add();

        CHANNEL.messageBuilder(WildshapeCantripHeldMessage.class, nextPacketId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(WildshapeCantripHeldMessage::encode)
                .decoder(WildshapeCantripHeldMessage::decode)
                .consumerMainThread(WildshapeCantripHeldMessage::handle)
                .add();

        CHANNEL.messageBuilder(RequestContainerSyncMessage.class, nextPacketId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestContainerSyncMessage::encode)
                .decoder(RequestContainerSyncMessage::decode)
                .consumerMainThread(RequestContainerSyncMessage::handle)
                .add();

        CHANNEL.messageBuilder(RequestFullSyncMessage.class, nextPacketId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestFullSyncMessage::encode)
                .decoder(RequestFullSyncMessage::decode)
                .consumerMainThread(RequestFullSyncMessage::handle)
                .add();

        CHANNEL.messageBuilder(RequestServerStateDumpMessage.class, nextPacketId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestServerStateDumpMessage::encode)
                .decoder(RequestServerStateDumpMessage::decode)
                .consumerMainThread(RequestServerStateDumpMessage::handle)
                .add();

        CHANNEL.messageBuilder(SyncSelectionSessionMessage.class, nextPacketId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncSelectionSessionMessage::encode)
                .decoder(SyncSelectionSessionMessage::decode)
                .consumerMainThread(SyncSelectionSessionMessage::handle)
                .add();

        CHANNEL.messageBuilder(SelectionSessionFinishedMessage.class, nextPacketId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(SelectionSessionFinishedMessage::encode)
                .decoder(SelectionSessionFinishedMessage::decode)
                .consumerMainThread(SelectionSessionFinishedMessage::handle)
                .add();

        CHANNEL.messageBuilder(BeginReselectionMessage.class, nextPacketId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(BeginReselectionMessage::encode)
                .decoder(BeginReselectionMessage::decode)
                .consumerMainThread(BeginReselectionMessage::handle)
                .add();
    }
}