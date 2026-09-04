package dev.muon.raven_dnd_origins.power;

import com.mojang.serialization.MapCodec;
import dev.muon.raven_core.leveling.LevelingUtils;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.shared.EmptyCfg;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.OptionalInt;

/**
 * Exposes the player's JustLevelingFork level as a read-only resource, for use with
 * {@code apoli:resource} comparisons in conditions.
 */
public final class PlayerLevelPower extends PowerType<EmptyCfg> {
    @Override
    public MapCodec<EmptyCfg> configCodec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    @Override
    public OptionalInt readResource(ResourceLocation powerId, EmptyCfg cfg, PowerContainer holder) {
        return holder.rawOwner() instanceof Player player
                ? OptionalInt.of(LevelingUtils.getPlayerLevel(player))
                : OptionalInt.of(0);
    }

    @Override
    public OptionalInt resourceBound(ResourceLocation powerId, EmptyCfg cfg, PowerContainer holder, boolean max) {
        return max ? OptionalInt.of(Integer.MAX_VALUE) : OptionalInt.of(0);
    }
}
