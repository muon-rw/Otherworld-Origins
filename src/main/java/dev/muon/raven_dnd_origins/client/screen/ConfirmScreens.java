package dev.muon.raven_dnd_origins.client.screen;

import dev.muon.raven_dnd_origins.selection.SessionKind;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Entry point for the clientbound confirm-screen payload. The payload class is linked on the
 * dedicated server too, so it must not construct screens itself: a static call with common-typed
 * arguments is the only reference the verifier tolerates there.
 */
public final class ConfirmScreens {
    private ConfirmScreens() {}

    public static void open(SessionKind kind, List<ResourceLocation> layers) {
        Minecraft mc = Minecraft.getInstance();
        if (kind == SessionKind.RESELECTION) {
            mc.setScreen(new ScopedConfirmScreen(layers));
        } else {
            mc.setScreen(new FinalConfirmScreen());
        }
    }
}
