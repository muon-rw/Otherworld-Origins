package dev.muon.raven_dnd_origins.client.shapeshift;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = RavenDndOrigins.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ShapeshiftClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        if (ModList.get().isLoaded("geckolib")) {
            GeckoLibAnimationSync.register();
        }
        if (ModList.get().isLoaded("iceandfire")) {
            IceAndFireAnimationSync.register();
        }
    }
}
