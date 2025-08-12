package dev.muon.otherworldorigins;

import com.mojang.logging.LogUtils;
import dev.muon.otherworldorigins.action.ModActions;
import dev.muon.otherworldorigins.condition.ModConditions;
import dev.muon.otherworldorigins.config.OtherworldOriginsConfig;
import dev.muon.otherworldorigins.effect.ModEffects;
import dev.muon.otherworldorigins.entity.ModEntities;
import dev.muon.otherworldorigins.item.ModItems;
import dev.muon.otherworldorigins.network.*;
import dev.muon.otherworldorigins.power.ModPowers;
import dev.muon.otherworldorigins.restrictions.SpellCategoryMapper;
import dev.muon.otherworldorigins.restrictions.SpellRestrictions;
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

    public static ResourceLocation loc(String id) {
        return new ResourceLocation(MODID, id);
    }

    public static final Logger LOGGER = LogUtils.getLogger();

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MODID, "main"),
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

        ModEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModEffects.register(modEventBus);

        ModActions.register(modEventBus);
        ModConditions.register(modEventBus);
        ModPowers.register(modEventBus);

        ModSpells.register(modEventBus);
        ModPassives.register(modEventBus);
        ModSkills.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);

        registerMessages();
    }

    private void onConfigLoad(final ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == OtherworldOriginsConfig.SPEC) {
            SpellRestrictions.initializeFromConfig();
        }
    }
    private void onConfigReload(final ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == OtherworldOriginsConfig.SPEC) {
            SpellRestrictions.initializeFromConfig();
            SpellCategoryMapper.initialize();
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        SpellCategoryMapper.initialize();
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

        CHANNEL.messageBuilder(CheckFeatScreenMessage.class, nextPacketId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(CheckFeatScreenMessage::encode)
                .decoder(CheckFeatScreenMessage::decode)
                .consumerMainThread(CheckFeatScreenMessage::handle)
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
        CHANNEL.messageBuilder(SendFeatLayersMessage.class, nextPacketId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SendFeatLayersMessage::encode)
                .decoder(SendFeatLayersMessage::decode)
                .consumerMainThread(SendFeatLayersMessage::handle)
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
    }
}