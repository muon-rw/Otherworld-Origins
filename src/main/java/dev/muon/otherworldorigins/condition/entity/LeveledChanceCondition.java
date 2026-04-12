package dev.muon.otherworldorigins.condition.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.otherworldorigins.util.LeveledScaling;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityCondition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class LeveledChanceCondition extends EntityCondition<LeveledChanceCondition.Configuration> {

    public LeveledChanceCondition() {
        super(Configuration.CODEC);
    }

    @Override
    public boolean check(@NotNull Configuration configuration, @NotNull Entity entity) {
        if (!LeveledScaling.isValidAptitudeReference(configuration.aptitude())) {
            return false;
        }
        int level = LeveledScaling.levelForScaling(entity, configuration.aptitude());
        float chance = configuration.base() + configuration.perLevel() * level;
        chance = Mth.clamp(chance, 0f, 1f);
        return entity.level().getRandom().nextFloat() < chance;
    }

    public record Configuration(float base, float perLevel, Optional<ResourceLocation> aptitude) implements IDynamicFeatureConfiguration {
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
