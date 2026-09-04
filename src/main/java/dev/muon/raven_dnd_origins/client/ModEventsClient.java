package dev.muon.raven_dnd_origins.client;

import com.crispytwig.naturalist.client.renderer.BearRenderer;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.client.compat.appleskin.AppleSkinHungerImmunityCompat;
import dev.muon.raven_dnd_origins.client.network.ShapeshiftSyncClient;
import dev.muon.raven_dnd_origins.entity.ModEntities;
import dev.muon.raven_dnd_origins.item.HeartsTooltipComponent;
import dev.muon.raven_dnd_origins.network.ShapeshiftSyncClientDispatch;
import net.minecraft.client.renderer.entity.IronGolemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = RavenDndOrigins.MODID, bus = EventBusSubscriber.Bus.MOD)
@OnlyIn(Dist.CLIENT)
public class ModEventsClient {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SUMMONED_IRON_GOLEM.get(), IronGolemRenderer::new);
        if (ModEntities.SUMMONED_GRIZZLY_BEAR != null) {
            event.registerEntityRenderer(ModEntities.SUMMONED_GRIZZLY_BEAR.get(), BearRenderer::new);
        }
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ShapeshiftSyncClientDispatch.registerHandler(ShapeshiftSyncClient::handlePacket);

        // Register our animation layer for player-animator. Priority 100 to avoid conflicts with Iron's (spell casting).
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                PlayerAnimationHandler.ANIMATION_LAYER_ID,
                100,
                player -> new ModifierLayer<IAnimation>()
        );

        if (ModList.get().isLoaded("appleskin")) {
            AppleSkinHungerImmunityCompat.init();
        }
    }

    @SubscribeEvent
    public static void registerKeyBindings(RegisterKeyMappingsEvent event) {
        event.register(ModKeybinds.CANTRIP_RACE_KEY);
        event.register(ModKeybinds.CANTRIP_ONE_KEY);
        event.register(ModKeybinds.CANTRIP_TWO_KEY);
        event.register(ModKeybinds.CANTRIP_THREE_KEY);
        event.register(ModKeybinds.TOGGLE_DARK_VISION_KEY);
    }

    @SubscribeEvent
    public static void registerTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(HeartsTooltipComponent.class, HeartsTooltipRenderer::new);
    }
}
