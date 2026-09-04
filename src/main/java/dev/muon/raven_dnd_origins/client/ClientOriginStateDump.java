package dev.muon.raven_dnd_origins.client;

import dev.muon.raven_dnd_origins.util.OriginStateDumper;
import net.minecraft.client.Minecraft;

/**
 * Client half of the state dump. Lives apart from the payload class so a dedicated server never
 * has to verify a {@code LocalPlayer} reference.
 */
public final class ClientOriginStateDump {
    private ClientOriginStateDump() {}

    public static void dump(String reason) {
        OriginStateDumper.dump(Minecraft.getInstance().player, "CLIENT", null, reason);
    }
}
