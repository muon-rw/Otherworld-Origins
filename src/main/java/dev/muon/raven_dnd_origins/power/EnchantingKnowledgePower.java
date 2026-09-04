package dev.muon.raven_dnd_origins.power;

import com.mojang.serialization.MapCodec;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import dev.overgrown.apoli.shared.EmptyCfg;
import net.minecraft.world.entity.player.Player;

/**
 * While active, Apotheosis enchanting-table clue packets list every enchantment in the
 * roll pool for each slot (same UX as maximum table clues / "all runes").
 */
public final class EnchantingKnowledgePower extends PowerType<EmptyCfg> {
    @Override
    public MapCodec<EmptyCfg> configCodec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    public static boolean has(Player player) {
        return PowerLookup.hasActive(player, ModPowers.ENCHANTING_KNOWLEDGE);
    }
}
