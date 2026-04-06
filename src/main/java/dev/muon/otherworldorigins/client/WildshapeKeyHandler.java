package dev.muon.otherworldorigins.client;

import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.network.WildshapeCantripHeldMessage;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = OtherworldOrigins.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WildshapeKeyHandler {

    private static boolean wasCantripHeld = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        boolean isCantripHeld = mc.screen == null && ModKeybinds.CANTRIP_THREE_KEY.isDown();
        if (isCantripHeld != wasCantripHeld) {
            OtherworldOrigins.CHANNEL.sendToServer(new WildshapeCantripHeldMessage(isCantripHeld));
            wasCantripHeld = isCantripHeld;
        }
    }
}
