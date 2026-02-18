package dev.muon.otherworldorigins.power;

import com.mojang.serialization.Codec;
import dev.muon.otherworld.leveling.LevelingUtils;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.IVariableIntPower;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class PlayerLevelPower extends PowerFactory<PlayerLevelPower.Configuration> implements IVariableIntPower<PlayerLevelPower.Configuration> {

    public PlayerLevelPower() {
        super(Configuration.CODEC);
    }

    @Override
    public int getValue(ConfiguredPower<Configuration, ?> configuration, Entity entity) {
        return entity instanceof Player p ? LevelingUtils.getPlayerLevel(p) : 0;
    }

    @Override
    public int getMinimum(ConfiguredPower<Configuration, ?> configuration, Entity entity) {
        return 0;
    }

    @Override
    public int getMaximum(ConfiguredPower<Configuration, ?> configuration, Entity entity) {
        return Integer.MAX_VALUE;
    }

    @Override
    public int assign(ConfiguredPower<Configuration, ?> configuration, Entity entity, int value) {
        return getValue(configuration, entity);
    }

    public record Configuration() implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = Codec.unit(Configuration::new);
    }
}