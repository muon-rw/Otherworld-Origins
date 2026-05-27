package dev.muon.otherworldorigins.power;

import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.configuration.NoConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import net.minecraft.world.entity.player.Player;

/**
 * Marker power. When the holder scoops or brews an Ars Elixirum elixir, the resulting
 * {@code ElixirContents} risk trait is forced to {@code Risk.PERFECT} (no side-effects).
 * Read by {@code ElixirScoopUpMixin}. Respects {@code isActive} so JSON-level conditions
 * (e.g. {@code player_level >= 15}) gate the perk correctly.
 */
public class SuppressElixirRiskPower extends PowerFactory<NoConfiguration> {

    public SuppressElixirRiskPower() {
        super(NoConfiguration.CODEC);
    }

    public static boolean has(Player player) {
        if (player == null) return false;
        IPowerContainer container = ApoliAPI.getPowerContainer(player);
        if (container == null) return false;
        return container.getPowers(ModPowers.SUPPRESS_ELIXIR_RISK.get()).stream()
                .anyMatch(holder -> holder.isBound() && holder.value().isActive(player));
    }
}
