package dev.muon.otherworldorigins.power;

import dev.muon.otherworld.power.PowerPresenceCache;
import io.github.edwinmindcraft.apoli.api.configuration.NoConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import net.minecraft.world.entity.Entity;

/**
 * Marker for undead subraces: mixin and HUD hooks gate on {@link #has(Entity)}.
 */
public class WalkOnPowderSnowPower extends PowerFactory<NoConfiguration> {
    public WalkOnPowderSnowPower() {
        super(NoConfiguration.CODEC);
    }

    public static boolean has(Entity entity) {
        if (entity == null) return false;
        return PowerPresenceCache.hasPower(entity, ModPowers.WALK_ON_POWDER_SNOW.get());
    }
}
