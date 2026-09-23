package dev.muon.raven_dnd_origins.power;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.shared.EmptyCfg;
import net.minecraft.world.entity.LivingEntity;

public final class BypassArmorWeightPower extends PowerType<EmptyCfg> {
    @Override
    public MapCodec<EmptyCfg> configCodec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    public static boolean has(LivingEntity entity) {
        return PowerLookup.hasActive(entity, ModPowers.BYPASS_ARMOR_WEIGHT);
    }
}
