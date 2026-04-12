package dev.muon.otherworldorigins.action.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.otherworldorigins.util.LeveledScaling;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityAction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public class LeveledHealAction extends EntityAction<LeveledHealAction.Configuration> {

    public LeveledHealAction() {
        super(Configuration.CODEC);
    }

    @Override
    public void execute(Configuration configuration, Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity) || entity.level().isClientSide()) {
            return;
        }

        int level = LeveledScaling.levelForScaling(livingEntity, configuration.aptitude());
        float healAmount = configuration.base() + (configuration.perLevel() * level);

        livingEntity.heal(healAmount);
    }

    public record Configuration(
            float base,
            float perLevel,
            Optional<ResourceLocation> aptitude
    ) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.fieldOf("base").forGetter(Configuration::base),
                Codec.FLOAT.fieldOf("per_level").forGetter(Configuration::perLevel),
                ResourceLocation.CODEC.optionalFieldOf("aptitude").forGetter(Configuration::aptitude)
        ).apply(instance, Configuration::new));

        @Override
        public boolean isConfigurationValid() {
            return LeveledScaling.isValidAptitudeReference(aptitude);
        }
    }
}
