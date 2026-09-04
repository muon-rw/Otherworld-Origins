package dev.muon.raven_dnd_origins.power;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.shared.EmptyCfg;

public final class PreventCriticalHitPower extends PowerType<EmptyCfg> {
    @Override
    public MapCodec<EmptyCfg> configCodec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }
}
