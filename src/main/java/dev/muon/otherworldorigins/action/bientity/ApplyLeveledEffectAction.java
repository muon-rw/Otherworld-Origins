package dev.muon.otherworldorigins.action.bientity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.otherworld.leveling.LevelingUtils;
import io.github.apace100.calio.data.SerializableDataTypes;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.configuration.ListConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.BiEntityAction;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Applies one or more status effects to the target. For each template, the final amplifier is
 * {@code template.amplifier + clamp(actorLevel - level_offset, min_amplifier, max_amplifier)}.
 * Non-player actors use level {@code 1}, matching {@link LevelingUtils#getPlayerLevel} usage elsewhere.
 */
public class ApplyLeveledEffectAction extends BiEntityAction<ApplyLeveledEffectAction.Configuration> {

    public ApplyLeveledEffectAction() {
        super(Configuration.CODEC);
    }

    @Override
    public void execute(Configuration configuration, Entity actor, Entity target) {
        if (!(target instanceof LivingEntity livingTarget) || actor.level().isClientSide()) {
            return;
        }

        int actorLevel = actor instanceof Player player ? LevelingUtils.getPlayerLevel(player) : 1;
        int scaled = actorLevel - configuration.levelOffset();
        int leveledAmplifier = Mth.clamp(scaled, configuration.minAmplifier(), configuration.maxAmplifier());

        for (MobEffectInstance template : configuration.effects().getContent()) {
            int amplifier = template.getAmplifier() + leveledAmplifier;
            livingTarget.addEffect(new MobEffectInstance(
                    template.getEffect(),
                    template.getDuration(),
                    amplifier,
                    template.isAmbient(),
                    template.isVisible(),
                    template.showIcon()));
        }
    }

    public record Configuration(
            ListConfiguration<MobEffectInstance> effects,
            int levelOffset,
            int minAmplifier,
            int maxAmplifier
    ) implements IDynamicFeatureConfiguration {
        public static final MapCodec<Configuration> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                        ListConfiguration.mapCodec(SerializableDataTypes.STATUS_EFFECT_INSTANCE, "effect", "effects")
                                .forGetter(Configuration::effects),
                        Codec.INT.optionalFieldOf("level_offset", 1).forGetter(Configuration::levelOffset),
                        Codec.INT.optionalFieldOf("min_amplifier", 0).forGetter(Configuration::minAmplifier),
                        Codec.INT.optionalFieldOf("max_amplifier", 19).forGetter(Configuration::maxAmplifier))
                .apply(instance, Configuration::new));

        public static final Codec<Configuration> CODEC = MAP_CODEC.codec();

        @Override
        public boolean isConfigurationValid() {
            return !effects.getContent().isEmpty();
        }
    }
}
