package dev.muon.otherworldorigins.action.bientity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.otherworldorigins.util.LeveledScaling;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.BiEntityAction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public class LeveledDamageBientityAction extends BiEntityAction<LeveledDamageBientityAction.Configuration> {

    public LeveledDamageBientityAction() {
        super(Configuration.CODEC);
    }

    @Override
    public void execute(Configuration configuration, Entity actor, Entity target) {
        if (!(actor instanceof LivingEntity livingActor) || !(target instanceof LivingEntity livingTarget) || actor.level().isClientSide()) {
            return;
        }

        int level = LeveledScaling.levelForScaling(actor, configuration.aptitude());
        float damage = configuration.base() + (configuration.perLevel() * level);
        if (damage <= 0) {
            return;
        }

        DamageSource source = resolveDamageSource(livingTarget, livingActor);
        livingTarget.hurt(source, damage);
    }

    private static DamageSource resolveDamageSource(LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player player) {
            return target.damageSources().playerAttack(player);
        }
        return target.damageSources().mobAttack(attacker);
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
