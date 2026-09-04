package dev.muon.raven_dnd_origins.power;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.shared.EmptyCfg;
import net.minecraft.world.entity.player.Player;

/**
 * While active, eldritch-school spells are treated as learned without changing stored learned-spell data
 * (see {@link dev.muon.raven_dnd_origins.mixin.compat.irons_spellbooks.SyncedSpellDataMixin}).
 */
public class EldritchKnowledgePower extends PowerType<EmptyCfg> {

    @Override
    public MapCodec<EmptyCfg> configCodec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    public static boolean has(Player player) {
        return PowerLookup.hasActive(player, ModPowers.ELDRITCH_KNOWLEDGE);
    }
}
