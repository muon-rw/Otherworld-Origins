package dev.muon.otherworldorigins;

import com.mojang.logging.LogUtils;
import dev.muon.otherworldorigins.action.ModActions;
import dev.muon.otherworldorigins.client.OtherworldOriginsClient;
import dev.muon.otherworldorigins.condition.ModConditions;
import dev.muon.otherworldorigins.effect.ModEffects;
import dev.muon.otherworldorigins.entity.ModEntities;
import dev.muon.otherworldorigins.item.ModItems;
import dev.muon.otherworldorigins.network.*;
import dev.muon.otherworldorigins.power.ModPowers;
import dev.muon.otherworldorigins.spells.ModSpells;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
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


    public OtherworldOrigins() {
        OtherworldOrigins.LOGGER.info("Loading Otherworld Origins");

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);

        ModEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModEffects.register(modEventBus);

        ModActions.register(modEventBus);
        ModConditions.register(modEventBus);
        ModPowers.register(modEventBus);

        ModSpells.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);

        registerMessages();
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
    }
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    @OnlyIn(Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
        }

        @SubscribeEvent
        public static void registerKeyBindings(RegisterKeyMappingsEvent event) {
            event.register(OtherworldOriginsClient.CANTRIP_ONE_KEY);
            event.register(OtherworldOriginsClient.CANTRIP_TWO_KEY);
            event.register(OtherworldOriginsClient.TOGGLE_DARK_VISION_KEY);
        }
    }
}