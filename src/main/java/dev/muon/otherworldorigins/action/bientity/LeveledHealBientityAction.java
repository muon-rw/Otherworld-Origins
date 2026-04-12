package dev.muon.otherworldorigins.action.bientity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.otherworldorigins.util.LeveledScaling;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.BiEntityAction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public class LeveledHealBientityAction extends BiEntityAction<LeveledHealBientityAction.Configuration> {

    public LeveledHealBientityAction() {
        super(Configuration.CODEC);
    }

    @Override
    public void execute(Configuration configuration, Entity actor, Entity target) {
        if (!(target instanceof LivingEntity livingTarget) || actor.level().isClientSide()) {
            return;
        }

        int level = LeveledScaling.levelForScaling(actor, configuration.aptitude());
        float healAmount = configuration.base() + (configuration.perLevel() * level);

        livingTarget.heal(healAmount);
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
