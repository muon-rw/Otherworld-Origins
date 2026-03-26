package dev.muon.otherworldorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import net.minecraft.world.entity.player.Player;

/**
 * Increases {@link net.minecraft.world.entity.Entity#getMaxAirSupply} while active (mixin on Entity).
 */
public class ModifyMaxAirSupplyPower extends PowerFactory<ModifyMaxAirSupplyPower.Configuration> {
    public ModifyMaxAirSupplyPower() {
        super(Configuration.CODEC);
    }

    /** Sum of {@code bonus} from all active {@link ModPowers#MODIFY_MAX_AIR_SUPPLY} powers on the player. */
    public static int getTotalAirBonus(Player player) {
        IPowerContainer container = ApoliAPI.getPowerContainer(player);
        if (container == null) {
            return 0;
        }
        return container.getPowers(ModPowers.MODIFY_MAX_AIR_SUPPLY.get()).stream()
                .map(holder -> holder.value().getConfiguration())
                .mapToInt(Configuration::bonus)
                .sum();
    }

    public record Configuration(int bonus) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("bonus").forGetter(Configuration::bonus)
        ).apply(instance, Configuration::new));

        @Override
        public boolean isConfigurationValid() {
            return bonus >= 0;
        }
    }
}
