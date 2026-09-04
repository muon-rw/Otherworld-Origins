package dev.muon.raven_dnd_origins.power;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.shared.EmptyCfg;
import net.minecraft.world.entity.player.Player;

/**
 * Marker power. LocalPlayerMixin skips the vanilla 0.2x forward/strafe input multiplier
 * in {@link net.minecraft.client.player.LocalPlayer#aiStep()} while this power is active.
 */
public final class PreventItemSlowdownPower extends PowerType<EmptyCfg> {
    @Override
    public MapCodec<EmptyCfg> configCodec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    public static boolean has(Player player) {
        return PowerLookup.hasActive(player, ModPowers.PREVENT_ITEM_SLOWDOWN);
    }
}
