package dev.muon.otherworldorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredBiEntityAction;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredBiEntityCondition;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import io.github.edwinmindcraft.apoli.common.registry.condition.ApoliDefaultConditions;
import io.github.edwinmindcraft.calio.api.network.CalioCodecHelper;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Power that deflects projectiles on impact. Evaluates bientity_condition (actor=deflector, target=projectile)
 * when a projectile hits; deflecting bypasses the damage pipeline entirely.
 * Use origins:actor_condition to check an entity condition on the deflector.
 */
public class DeflectProjectilePower extends PowerFactory<DeflectProjectilePower.Configuration> {

    public DeflectProjectilePower() {
        super(Configuration.CODEC);
    }

    /**
     * Returns the first deflect power that passes its conditions for the given (deflector, projectile) pair.
     */
    public static Optional<ConfiguredPower<Configuration, ?>> tryDeflect(Entity deflector, Projectile projectile) {
        if (!(deflector instanceof net.minecraft.world.entity.LivingEntity) || deflector.level().isClientSide()) {
            return Optional.empty();
        }
        return IPowerContainer.get(deflector).resolve()
                .flatMap(container -> container.getPowers(ModPowers.DEFLECT_PROJECTILE.get()).stream()
                        .filter(holder -> {
                            DeflectProjectilePower factory = holder.value().getFactory();
                            return factory.check(holder.value(), deflector, projectile);
                        })
                        .map(Holder::value)
                        .findFirst());
    }

    /**
     * Executes the deflect: cancels impact and redirects the projectile.
     * Uses bientity_action if present, otherwise redirects toward the original shooter.
     */
    public static void executeDeflect(ConfiguredPower<Configuration, ?> config, Entity deflector, Projectile projectile) {
        Configuration configuration = config.getConfiguration();
        if (configuration.bientityAction().isPresent()) {
            ConfiguredBiEntityAction.execute(configuration.bientityAction().get(), deflector, projectile);
        } else {
            redirectProjectile(projectile, deflector);
        }
    }

    private static void redirectProjectile(@NotNull Projectile projectile, @NotNull Entity deflector) {
        Entity originalShooter = projectile.getOwner();
        net.minecraft.world.phys.Vec3 delta = projectile.getDeltaMovement();
        double speed = delta.length();

        net.minecraft.world.phys.Vec3 newDelta;
        if (originalShooter != null && originalShooter.isAlive()) {
            net.minecraft.world.phys.Vec3 toShooter = originalShooter.position().subtract(projectile.position());
            double dist = toShooter.length();
            if (dist > 1.0E-6) {
                newDelta = toShooter.normalize().scale(Math.max(speed, 0.1));
            } else {
                newDelta = new net.minecraft.world.phys.Vec3(-delta.x, delta.y, -delta.z);
            }
        } else {
            newDelta = new net.minecraft.world.phys.Vec3(-delta.x, delta.y, -delta.z);
        }

        projectile.setDeltaMovement(newDelta);
        projectile.setOwner(deflector);
    }

    public boolean check(ConfiguredPower<Configuration, ?> config, Entity deflector, Projectile projectile) {
        Configuration configuration = config.getConfiguration();
        if (configuration.bientityCondition().is(ApoliDefaultConditions.BIENTITY_DEFAULT.getId())) {
            return false;
        }
        return ConfiguredBiEntityCondition.check(configuration.bientityCondition(), deflector, projectile);
    }

    public record Configuration(
            Holder<ConfiguredBiEntityCondition<?, ?>> bientityCondition,
            Optional<Holder<ConfiguredBiEntityAction<?, ?>>> bientityAction
    ) implements IDynamicFeatureConfiguration {
        @Override
        public boolean isConfigurationValid() {
            return true;
        }

        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ConfiguredBiEntityCondition.required("bientity_condition").forGetter(Configuration::bientityCondition),
                CalioCodecHelper.optionalField(ConfiguredBiEntityAction.HOLDER, "bientity_action").forGetter(Configuration::bientityAction)
        ).apply(instance, Configuration::new));
    }
}
