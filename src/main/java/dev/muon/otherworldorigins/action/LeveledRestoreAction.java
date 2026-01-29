package dev.muon.otherworldorigins.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.otherworld.leveling.LevelingUtils;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityAction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class LeveledRestoreAction extends EntityAction<LeveledRestoreAction.Configuration> {

    public LeveledRestoreAction() {
        super(Configuration.CODEC);
    }

    @Override
    public void execute(Configuration configuration, Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity) || entity.level().isClientSide()) {
            return;
        }

        int level = livingEntity instanceof Player player ? LevelingUtils.getPlayerLevel(player) : 1;
        float healAmount = configuration.base() + (configuration.perLevel() * level);
        
        livingEntity.heal(healAmount);
    }

    public record Configuration(
            float base,
            float perLevel
    ) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.fieldOf("base").forGetter(Configuration::base),
                Codec.FLOAT.fieldOf("per_level").forGetter(Configuration::perLevel)
        ).apply(instance, Configuration::new));

        @Override
        public boolean isConfigurationValid() {
            return true;
        }
    }
}

