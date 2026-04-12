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

/**
 * Adds absorption health (yellow hearts): one full heart equals {@value #HP_PER_HEART} absorption HP
 * (same units as {@link LivingEntity#getAbsorptionAmount()}).
 *
 * <p>Let {@code G} be the grant in absorption HP and {@code C} the ceiling in absorption HP (defaults to
 * {@code G} when {@code max_hearts} fields are omitted). Current absorption is {@code A}. The new amount is
 * {@code max(A, min(A + G, C))}: never lower than before, never above the ceiling, add up to {@code G} toward
 * that ceiling. Other sources that already put {@code A} above {@code C} are left unchanged.
 */
public class GrantAbsorptionHeartsAction extends EntityAction<GrantAbsorptionHeartsAction.Configuration> {

    /** Full hearts to absorption HP (vanilla health points). */
    public static final float HP_PER_HEART = 2.0f;

    public GrantAbsorptionHeartsAction() {
        super(Configuration.CODEC);
    }

    @Override
    public void execute(Configuration configuration, Entity entity) {
        if (!(entity instanceof LivingEntity living) || entity.level().isClientSide()) {
            return;
        }

        int level = LeveledScaling.levelForScaling(living, configuration.aptitude());
        float grantHearts = configuration.baseHearts() + configuration.perLevelHearts() * level;
        float ceilingHearts = configuration.resolveCeilingHearts(level, grantHearts);

        float grantHp = grantHearts * HP_PER_HEART;
        float ceilingHp = ceilingHearts * HP_PER_HEART;

        float current = living.getAbsorptionAmount();
        float updated = Math.max(current, Math.min(current + grantHp, ceilingHp));
        living.setAbsorptionAmount(Math.max(0.0f, updated));
    }

    public record Configuration(
            float baseHearts,
            float perLevelHearts,
            Optional<ResourceLocation> aptitude,
            Optional<Float> maxHeartsBase,
            Optional<Float> maxHeartsPerLevel
    ) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("base_hearts", 0.0f).forGetter(Configuration::baseHearts),
                Codec.FLOAT.optionalFieldOf("per_level_hearts", 0.0f).forGetter(Configuration::perLevelHearts),
                ResourceLocation.CODEC.optionalFieldOf("aptitude").forGetter(Configuration::aptitude),
                Codec.FLOAT.optionalFieldOf("max_hearts_base").forGetter(Configuration::maxHeartsBase),
                Codec.FLOAT.optionalFieldOf("max_hearts_per_level").forGetter(Configuration::maxHeartsPerLevel)
        ).apply(instance, Configuration::new));

        float resolveCeilingHearts(int level, float grantHearts) {
            if (maxHeartsBase.isEmpty() && maxHeartsPerLevel.isEmpty()) {
                return grantHearts;
            }
            float maxBase = maxHeartsBase.orElse(0.0f);
            float maxPer = maxHeartsPerLevel.orElse(0.0f);
            return maxBase + maxPer * level;
        }

        @Override
        public boolean isConfigurationValid() {
            return LeveledScaling.isValidAptitudeReference(aptitude);
        }
    }
}
