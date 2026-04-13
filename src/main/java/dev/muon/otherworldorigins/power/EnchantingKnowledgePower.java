package dev.muon.otherworldorigins.power;

import dev.muon.otherworld.power.PowerPresenceCache;
import io.github.edwinmindcraft.apoli.api.configuration.NoConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import net.minecraft.world.entity.player.Player;

/**
 * While active, Apotheosis enchanting-table clue packets list every enchantment in the
 * roll pool for each slot (same UX as maximum table clues / "all runes").
 */
public class EnchantingKnowledgePower extends PowerFactory<NoConfiguration> {
    public EnchantingKnowledgePower() {
        super(NoConfiguration.CODEC);
    }

    public static boolean has(Player player) {
        if (player == null) {
            return false;
        }
        return PowerPresenceCache.hasPower(player, ModPowers.ENCHANTING_KNOWLEDGE.get());
    }
}
