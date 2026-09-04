package dev.muon.raven_dnd_origins.client;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.network.WildshapeCantripHeldMessage;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(value = Dist.CLIENT, modid = RavenDndOrigins.MODID)
public class WildshapeKeyHandler {

    private static boolean wasCantripHeld = false;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        boolean isCantripHeld = mc.screen == null && ModKeybinds.CANTRIP_THREE_KEY.isDown();
        if (isCantripHeld != wasCantripHeld) {
            PacketDistributor.sendToServer(new WildshapeCantripHeldMessage(isCantripHeld));
            wasCantripHeld = isCantripHeld;
        }
    }
}
