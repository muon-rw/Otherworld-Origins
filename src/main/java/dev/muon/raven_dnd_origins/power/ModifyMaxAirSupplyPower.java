package dev.muon.raven_dnd_origins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.world.entity.player.Player;

/**
 * Increases {@link net.minecraft.world.entity.Entity#getMaxAirSupply} while active (mixin on Entity).
 */
public final class ModifyMaxAirSupplyPower extends PowerType<ModifyMaxAirSupplyPower.Configuration> {

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("bonus").forGetter(Configuration::bonus)
    ).apply(i, Configuration::new));

    @Override
    public MapCodec<Configuration> configCodec() {
        return CODEC;
    }

    /** Sum of {@code bonus} from all active {@link ModPowers#MODIFY_MAX_AIR_SUPPLY} powers on the player. */
    public static int getTotalAirBonus(Player player) {
        int[] total = new int[1];
        PowerLookup.forEach(player, ModPowers.MODIFY_MAX_AIR_SUPPLY, Configuration.class, cfg -> total[0] += cfg.bonus());
        return total[0];
    }

    public record Configuration(int bonus) {
    }
}
