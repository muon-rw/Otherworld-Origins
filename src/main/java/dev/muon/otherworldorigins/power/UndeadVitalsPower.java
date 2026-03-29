package dev.muon.otherworldorigins.power;

import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.configuration.NoConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import net.minecraft.world.entity.player.Player;

/**
 * Marker for undead subraces: mixin and HUD hooks gate on {@link #has(Player)}.
 */
public class UndeadVitalsPower extends PowerFactory<NoConfiguration> {
    public UndeadVitalsPower() {
        super(NoConfiguration.CODEC);
    }

    public static boolean has(Player player) {
        if (player == null) {
            return false;
        }
        IPowerContainer container = ApoliAPI.getPowerContainer(player);
        if (container == null) {
            return false;
        }
        return container.hasPower(ModPowers.UNDEAD_VITALS.get());
    }
}
