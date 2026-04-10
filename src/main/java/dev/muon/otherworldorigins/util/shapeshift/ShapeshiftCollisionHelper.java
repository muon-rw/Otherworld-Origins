package dev.muon.otherworldorigins.util.shapeshift;

import dev.muon.otherworldorigins.client.shapeshift.ShapeshiftClientState;
import dev.muon.otherworldorigins.power.ShapeshiftPower;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

public final class ShapeshiftCollisionHelper {

    private ShapeshiftCollisionHelper() {}

    /**
     * Resolved hitbox for physics and targeting. On the logical client, prefers values from the server
     * sync packet so remote players match the server; falls back to the local Apoli config when absent.
     * On the logical server (including integrated server's server side), uses only the power config.
     */
    @Nullable
    public static ShapeshiftCollisionShape resolve(Player player) {
        if (player.level().isClientSide()) {
            ShapeshiftCollisionShape fromSync = ShapeshiftClientState.getCollisionShape(player.getId());
            if (fromSync != null) {
                return fromSync;
            }
        }
        ShapeshiftPower.Configuration config = ShapeshiftPower.getActiveShapeshiftConfig(player);
        if (config != null) {
            return config.effectiveCollisionShape();
        }
        return null;
    }
}
