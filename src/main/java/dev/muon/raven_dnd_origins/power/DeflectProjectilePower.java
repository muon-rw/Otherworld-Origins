package dev.muon.raven_dnd_origins.power;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Power that deflects projectiles on impact. Evaluates bientity_condition (actor=deflector, target=projectile)
 * when a projectile hits; deflecting bypasses the damage pipeline entirely.
 * Use apoli:actor_condition to check an entity condition on the deflector.
 */
public class DeflectProjectilePower extends PowerType<DeflectProjectilePower.Configuration> {

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Configuration::bientityCondition),
            LoggedOptionalField.of("bientity_action", BiEntityAction.CODEC).forGetter(Configuration::bientityAction)
    ).apply(instance, Configuration::new));

    @Override
    public MapCodec<Configuration> configCodec() {
        return CODEC;
    }

    /**
     * Returns the first deflect power that passes its conditions for the given (deflector, projectile) pair.
     */
    public static Optional<Configuration> tryDeflect(Entity deflector, Projectile projectile) {
        if (!(deflector instanceof LivingEntity) || deflector.level().isClientSide()) {
            return Optional.empty();
        }
        for (Configuration cfg : PowerLookup.active(deflector, ModPowers.DEFLECT_PROJECTILE, Configuration.class)) {
            if (cfg.bientityCondition().isEmpty()) {
                continue;
            }
            if (cfg.bientityCondition().get().test(new BiEntityCtx(deflector, projectile, deflector.level()))) {
                return Optional.of(cfg);
            }
        }
        return Optional.empty();
    }

    /**
     * Executes the deflect: cancels impact and redirects the projectile.
     * Uses bientity_action if present, otherwise redirects toward the original shooter.
     */
    public static void executeDeflect(Configuration cfg, Entity deflector, Projectile projectile) {
        if (cfg.bientityAction().isPresent()) {
            cfg.bientityAction().get().run(new BiEntityCtx(deflector, projectile, deflector.level()));
        } else {
            redirectProjectile(projectile, deflector);
        }
    }

    private static void redirectProjectile(@NotNull Projectile projectile, @NotNull Entity deflector) {
        Entity originalShooter = projectile.getOwner();
        Vec3 delta = projectile.getDeltaMovement();
        double speed = delta.length();

        Vec3 newDelta;
        if (originalShooter != null && originalShooter.isAlive()) {
            Vec3 toShooter = originalShooter.position().subtract(projectile.position());
            double dist = toShooter.length();
            if (dist > 1.0E-6) {
                newDelta = toShooter.normalize().scale(Math.max(speed, 0.1));
            } else {
                newDelta = new Vec3(-delta.x, delta.y, -delta.z);
            }
        } else {
            newDelta = new Vec3(-delta.x, delta.y, -delta.z);
        }

        projectile.setDeltaMovement(newDelta);
        projectile.setOwner(deflector);
    }

    public record Configuration(
            Optional<BiEntityCondition> bientityCondition,
            Optional<BiEntityAction> bientityAction
    ) {}
}
