package dev.muon.otherworldorigins.client;

import com.github.alexthe666.alexsmobs.client.render.RenderGrizzlyBear;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.entity.ModEntities;
import net.minecraft.client.renderer.entity.IronGolemRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = OtherworldOrigins.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
@OnlyIn(Dist.CLIENT)
public class ModEventsClient {
    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SUMMONED_IRON_GOLEM.get(), IronGolemRenderer::new);
        event.registerEntityRenderer(ModEntities.SUMMONED_GRIZZLY_BEAR.get(), RenderGrizzlyBear::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Register our animation layer for player-animator. Priority 100 to avoid conflicts with Iron's (spell casting).
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                PlayerAnimationHandler.ANIMATION_LAYER_ID,
                100,
                player -> new ModifierLayer<IAnimation>()
        );
    }

    @SubscribeEvent
    public static void registerKeyBindings(RegisterKeyMappingsEvent event) {
        event.register(ModKeybinds.CANTRIP_ONE_KEY);
        event.register(ModKeybinds.CANTRIP_TWO_KEY);
        event.register(ModKeybinds.TOGGLE_DARK_VISION_KEY);
    }
}