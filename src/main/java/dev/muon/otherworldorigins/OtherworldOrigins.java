package dev.muon.otherworldorigins;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import dev.muon.otherworldorigins.action.ModActions;
import dev.muon.otherworldorigins.condition.ModConditions;
import dev.muon.otherworldorigins.entity.ModEntities;
import dev.muon.otherworldorigins.item.ModItems;
import dev.muon.otherworldorigins.network.CloseCurrentScreenMessage;
import dev.muon.otherworldorigins.network.ResetOriginsMessage;
import dev.muon.otherworldorigins.power.ModPowers;
import dev.muon.otherworldorigins.sounds.ModSounds;
import dev.muon.otherworldorigins.spells.ModSpells;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
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
import org.lwjgl.glfw.GLFW;
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

    public static final KeyMapping CANTRIP_ONE_KEY = new KeyMapping(
            "key.otherworldorigins.cantrip_one",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            "key.categories.otherworldorigins"
    );
    public static final KeyMapping CANTRIP_TWO_KEY = new KeyMapping(
            "key.otherworldorigins.cantrip_two",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            "key.categories.otherworldorigins"
    );
    public static final KeyMapping TOGGLE_DARK_VISION_KEY = new KeyMapping(
            "key.otherworldorigins.toggle_dark_vision",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            "key.categories.otherworldorigins"
    );

    public OtherworldOrigins() {
        OtherworldOrigins.LOGGER.info("Loading Otherworld Origins");

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);

        ModEntities.register(modEventBus);
        ModSounds.register(modEventBus);
        ModItems.register(modEventBus);

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
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    @OnlyIn(Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
        }

        @SubscribeEvent
        public static void registerKeyBindings(RegisterKeyMappingsEvent event) {
            event.register(CANTRIP_ONE_KEY);
            event.register(CANTRIP_TWO_KEY);
            event.register(TOGGLE_DARK_VISION_KEY);
        }
    }
}