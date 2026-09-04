package dev.muon.raven_dnd_origins.power;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.shared.EmptyCfg;
import net.minecraft.world.entity.Entity;

/**
 * Marker for undead subraces: mixin and HUD hooks gate on {@link #has(Entity)}.
 */
public final class WalkOnPowderSnowPower extends PowerType<EmptyCfg> {
    @Override
    public MapCodec<EmptyCfg> configCodec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    public static boolean has(Entity entity) {
        return PowerLookup.hasActive(entity, ModPowers.WALK_ON_POWDER_SNOW);
    }
}
