package dev.muon.otherworldorigins.power;

import dev.muon.otherworld.power.PowerPresenceCache;
import io.github.edwinmindcraft.apoli.api.configuration.NoConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import net.minecraft.world.entity.player.Player;

/**
 * Marker for undead subraces: mixin and HUD hooks gate on {@link #has(Player)}.
 */
public class HungerImmunityPower extends PowerFactory<NoConfiguration> {
    public HungerImmunityPower() {
        super(NoConfiguration.CODEC);
    }

    public static boolean has(Player player) {
        if (player == null) return false;
        return PowerPresenceCache.hasPower(player, ModPowers.HUNGER_IMMUNITY.get());
    }
}
