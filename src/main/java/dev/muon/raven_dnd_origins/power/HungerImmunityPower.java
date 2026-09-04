package dev.muon.raven_dnd_origins.power;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.shared.EmptyCfg;
import net.minecraft.world.entity.player.Player;

/**
 * Marker for undead subraces: mixin and HUD hooks gate on {@link #has(Player)}.
 */
public final class HungerImmunityPower extends PowerType<EmptyCfg> {
    @Override
    public MapCodec<EmptyCfg> configCodec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    public static boolean has(Player player) {
        return PowerLookup.hasActive(player, ModPowers.HUNGER_IMMUNITY);
    }
}
