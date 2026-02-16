package dev.muon.otherworldorigins.action.bientity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.apace100.calio.data.SerializableDataTypes;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.BiEntityAction;
import io.github.edwinmindcraft.calio.api.network.CalioCodecHelper;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class RaycastBetweenAction extends BiEntityAction<RaycastBetweenAction.Configuration> {

    public RaycastBetweenAction() {
        super(Configuration.CODEC);
    }

    @Override
    public void execute(Configuration configuration, Entity actor, Entity target) {
        if (actor == null || target == null || actor.level().isClientSide()) {
            return;
        }
        createParticlesAtHitPos(actor, new EntityHitResult(target), configuration.particle(), configuration.spacing());
    }

    protected static Vec3 createDirectionVector(Vec3 pos1, Vec3 pos2) {
        return new Vec3(pos2.x() - pos1.x(), pos2.y() - pos1.y(), pos2.z() - pos1.z()).normalize();
    }

    protected static void createParticlesAtHitPos(Entity entity, HitResult hitResult, ParticleOptions particle, double spacing) {
        if (entity.level().isClientSide()) return;

        double distanceTo = hitResult.distanceTo(entity);

        for (double d = spacing; d < distanceTo; d += spacing) {
            double lerpValue = Mth.clamp(d / distanceTo, 0.0, 1.0);
            ((ServerLevel) entity.level()).sendParticles(
                    particle,
                    Mth.lerp(lerpValue, entity.getEyePosition().x(), hitResult.getLocation().x()),
                    Mth.lerp(lerpValue, entity.getEyePosition().y(), hitResult.getLocation().y()),
                    Mth.lerp(lerpValue, entity.getEyePosition().z(), hitResult.getLocation().z()),
                    1, 0, 0, 0, 0
            );
        }
    }

    public record Configuration(
            ParticleOptions particle,
            double spacing
    ) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                SerializableDataTypes.PARTICLE_EFFECT_OR_TYPE.fieldOf("particle").forGetter(Configuration::particle),
                CalioCodecHelper.optionalField(CalioCodecHelper.DOUBLE, "spacing", 0.5).forGetter(Configuration::spacing)
        ).apply(instance, Configuration::new));

        @Override
        public boolean isConfigurationValid() {
            return true;
        }
    }
}
