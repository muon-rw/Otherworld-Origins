package dev.muon.otherworldorigins.power;

import dev.muon.otherworld.power.PowerPresenceCache;
import io.github.edwinmindcraft.apoli.api.configuration.NoConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import net.minecraft.world.entity.player.Player;

/**
 * Marker power. LocalPlayerMixin skips the vanilla 0.2x forward/strafe input multiplier
 * in {@link net.minecraft.client.player.LocalPlayer#aiStep()} while this power is active.
 */
public class PreventItemSlowdownPower extends PowerFactory<NoConfiguration> {
    public PreventItemSlowdownPower() {
        super(NoConfiguration.CODEC);
    }

    public static boolean has(Player player) {
        if (player == null) return false;
        return PowerPresenceCache.hasPower(player, ModPowers.PREVENT_ITEM_SLOWDOWN.get());
    }
}
