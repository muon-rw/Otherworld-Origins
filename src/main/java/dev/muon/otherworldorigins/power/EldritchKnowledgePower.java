package dev.muon.otherworldorigins.power;

import dev.muon.otherworld.power.PowerPresenceCache;
import io.github.edwinmindcraft.apoli.api.configuration.NoConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import net.minecraft.world.entity.player.Player;

/**
 * While active, eldritch-school spells are treated as learned without changing stored learned-spell data
 * (see {@link dev.muon.otherworldorigins.mixin.compat.irons_spellbooks.SyncedSpellDataMixin}).
 */
public class EldritchKnowledgePower extends PowerFactory<NoConfiguration> {
    public EldritchKnowledgePower() {
        super(NoConfiguration.CODEC);
    }

    public static boolean has(Player player) {
        if (player == null) {
            return false;
        }
        return PowerPresenceCache.hasPower(player, ModPowers.ELDRITCH_KNOWLEDGE.get());
    }
}
