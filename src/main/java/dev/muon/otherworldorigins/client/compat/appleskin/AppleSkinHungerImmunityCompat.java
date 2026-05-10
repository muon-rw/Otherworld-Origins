package dev.muon.otherworldorigins.client.compat.appleskin;

import dev.muon.otherworldorigins.power.HungerImmunityPower;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import squeek.appleskin.api.event.HUDOverlayEvent;

/**
 * AppleSkin draws its saturation overlay and exhaustion underlay on the hunger bar without
 * checking whether the hunger bar itself is being rendered. {@link dev.muon.otherworldorigins.mixin.client.ForgeGuiMixin}
 * suppresses the hunger bar for hunger-immune players, so the AppleSkin overlays end up
 * floating in empty space. Cancel the cancellable events AppleSkin posts to keep them in sync.
 */
public class AppleSkinHungerImmunityCompat {

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new AppleSkinHungerImmunityCompat());
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
