package dev.muon.otherworldorigins;

import com.mojang.logging.LogUtils;
import dev.muon.otherworldorigins.action.ModActions;
import dev.muon.otherworldorigins.effect.ModEffects;
import dev.muon.otherworldorigins.condition.ModConditions;
import dev.muon.otherworldorigins.config.OtherworldOriginsConfig;
import dev.muon.otherworldorigins.attribute.ModAttributes;
import dev.muon.otherworldorigins.entity.ModEntities;
import dev.muon.otherworldorigins.item.ModItems;
import dev.muon.otherworldorigins.network.*;
import dev.muon.otherworldorigins.power.ModPowers;
import dev.muon.otherworldorigins.school.ModSchools;
import dev.muon.otherworldorigins.skills.ModPassives;
import dev.muon.otherworldorigins.skills.ModSkills;
import dev.muon.otherworldorigins.spells.ModSpells;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.slf4j.Logger;

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
        context.registerConfig(ModConfig.Type.COMMON, OtherworldOriginsConfig.SPEC);

        IEventBus modEventBus = context.getModEventBus();
        modEventBus.addListener(this::onConfigLoad);
        modEventBus.addListener(this::onConfigReload);
        modEventBus.addListener(this::commonSetup);

        ModAttributes.register(modEventBus);
        ModEntities.register(modEventBus);
        ModEffects.register(modEventBus);
        ModItems.register(modEventBus);

        ModActions.register(modEventBus);
        ModConditions.register(modEventBus);
        ModPowers.register(modEventBus);

        ModSchools.register(modEventBus);
        ModSpells.register(modEventBus);

        ModPassives.register(modEventBus);
        ModSkills.register(modEventBus);


        MinecraftForge.EVENT_BUS.register(this);

        registerMessages();
    }

    private void onConfigLoad(final ModConfigEvent.Loading event) {
    }
    private void onConfigReload(final ModConfigEvent.Reloading event) {
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
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

        CHANNEL.messageBuilder(CheckLeveledLayersMessage.class, nextPacketId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(CheckLeveledLayersMessage::encode)
                .decoder(CheckLeveledLayersMessage::decode)
                .consumerMainThread(CheckLeveledLayersMessage::handle)
                .add();
        CHANNEL.messageBuilder(RespecAptitudesMessage.class, nextPacketId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(RespecAptitudesMessage::encode)
                .decoder(RespecAptitudesMessage::decode)
                .consumerMainThread(RespecAptitudesMessage::handle)
                .add();
        CHANNEL.messageBuilder(RequestLayerValidationMessage.class, nextPacketId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestLayerValidationMessage::encode)
                .decoder(RequestLayerValidationMessage::decode)
                .consumerMainThread(RequestLayerValidationMessage::handle)
                .add();

        CHANNEL.messageBuilder(SendValidatedLayersMessage.class, nextPacketId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SendValidatedLayersMessage::encode)
                .decoder(SendValidatedLayersMessage::decode)
                .consumerMainThread(SendValidatedLayersMessage::handle)
                .add();
        CHANNEL.messageBuilder(SendLeveledLayersMessage.class, nextPacketId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SendLeveledLayersMessage::encode)
                .decoder(SendLeveledLayersMessage::decode)
                .consumerMainThread(SendLeveledLayersMessage::handle)
                .add();
        CHANNEL.messageBuilder(ResetValidationAttemptsMessage.class, nextPacketId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ResetValidationAttemptsMessage::encode)
                .decoder(ResetValidationAttemptsMessage::decode)
                .consumerMainThread(ResetValidationAttemptsMessage::handle)
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
    }
}