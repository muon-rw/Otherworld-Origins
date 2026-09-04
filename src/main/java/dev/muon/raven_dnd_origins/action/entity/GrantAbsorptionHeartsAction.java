package dev.muon.raven_dnd_origins.action.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.raven_dnd_origins.util.LeveledScaling;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.resources.ResourceLocation;
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
public final class GrantAbsorptionHeartsAction implements ActionType<EntityCtx, GrantAbsorptionHeartsAction.Cfg> {

    public static final float HP_PER_HEART = 2.0f;

    public record Cfg(
            float baseHearts,
            float perLevelHearts,
            Optional<ResourceLocation> aptitude,
            Optional<Float> maxHeartsBase,
            Optional<Float> maxHeartsPerLevel
    ) {
        float resolveCeilingHearts(int level, float grantHearts) {
            if (maxHeartsBase.isEmpty() && maxHeartsPerLevel.isEmpty()) {
                return grantHearts;
            }
            float maxBase = maxHeartsBase.orElse(0.0f);
            float maxPer = maxHeartsPerLevel.orElse(0.0f);
            return maxBase + maxPer * level;
        }
    }

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.FLOAT.optionalFieldOf("base_hearts", 0.0f).forGetter(Cfg::baseHearts),
                Codec.FLOAT.optionalFieldOf("per_level_hearts", 0.0f).forGetter(Cfg::perLevelHearts),
                ResourceLocation.CODEC.optionalFieldOf("aptitude").forGetter(Cfg::aptitude),
                Codec.FLOAT.optionalFieldOf("max_hearts_base").forGetter(Cfg::maxHeartsBase),
                Codec.FLOAT.optionalFieldOf("max_hearts_per_level").forGetter(Cfg::maxHeartsPerLevel)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        LivingEntity living = ctx.living();
        if (living == null) {
            return;
        }

        int level = LeveledScaling.levelForScaling(living, cfg.aptitude());
        float grantHearts = cfg.baseHearts() + cfg.perLevelHearts() * level;
        float ceilingHearts = cfg.resolveCeilingHearts(level, grantHearts);

        float grantHp = grantHearts * HP_PER_HEART;
        float ceilingHp = ceilingHearts * HP_PER_HEART;

        float current = living.getAbsorptionAmount();
        float updated = Math.max(current, Math.min(current + grantHp, ceilingHp));
        living.setAbsorptionAmount(Math.max(0.0f, updated));
    }
}
