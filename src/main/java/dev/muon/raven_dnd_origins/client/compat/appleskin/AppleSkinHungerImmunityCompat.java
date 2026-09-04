package dev.muon.raven_dnd_origins.client.compat.appleskin;

import dev.muon.raven_dnd_origins.power.HungerImmunityPower;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import squeek.appleskin.api.event.HUDOverlayEvent;

/**
 * AppleSkin draws its saturation overlay and exhaustion underlay on the hunger bar without
 * checking whether the hunger bar itself is being rendered. Our food-bar gui layer suppresses the
 * hunger bar for hunger-immune players, so the AppleSkin overlays end up floating in empty space.
 * Cancel the cancellable events AppleSkin posts to keep them in sync.
 */
public class AppleSkinHungerImmunityCompat {

    public static void init() {
        NeoForge.EVENT_BUS.register(new AppleSkinHungerImmunityCompat());
    }

    @SubscribeEvent
    public void onSaturationOverlay(HUDOverlayEvent.Saturation event) {
        if (HungerImmunityPower.has(Minecraft.getInstance().player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onExhaustionOverlay(HUDOverlayEvent.Exhaustion event) {
        if (HungerImmunityPower.has(Minecraft.getInstance().player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onHungerRestoredOverlay(HUDOverlayEvent.HungerRestored event) {
        if (HungerImmunityPower.has(Minecraft.getInstance().player)) {
            event.setCanceled(true);
        }
    }
}
