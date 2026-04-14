package dev.muon.otherworldorigins.power;

import dev.muon.otherworld.power.PowerPresenceCache;
import io.github.edwinmindcraft.apoli.api.configuration.NoConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import net.minecraft.world.entity.player.Player;

public class ShowRepairTooltipPower extends PowerFactory<NoConfiguration> {
    public ShowRepairTooltipPower() {
        super(NoConfiguration.CODEC);
    }

    public static boolean has(Player player) {
        if (player == null) {
            return false;
        }
        return PowerPresenceCache.hasPower(player, ModPowers.SHOW_REPAIR_TOOLTIP.get());
    }
}
