package dev.muon.raven_dnd_origins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Pushes the arrow's owner opposite the arrow's post-shoot delta movement. Called once per
 * real {@code shoot()} (multishot clones bypass {@code shoot()}, so the recoil stays per-shot
 * even when Hail of Arrows spawns extras).
 */
public class MomentumPower extends PowerType<MomentumPower.Configuration> {

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.FLOAT.optionalFieldOf("strength", 0.6F).forGetter(Configuration::strength),
            LoggedOptionalField.of("self_action", EntityAction.CODEC).forGetter(Configuration::selfAction)
    ).apply(instance, Configuration::new));

    @Override
    public MapCodec<Configuration> configCodec() {
        return CODEC;
    }

    public static void applyRecoil(AbstractArrow source, Entity owner) {
        if (owner.level().isClientSide() || !(owner instanceof LivingEntity)) {
            return;
        }
        PowerLookup.forEach(owner, ModPowers.MOMENTUM, Configuration.class, cfg -> {
            push(source, owner, cfg.strength());
            cfg.selfAction().ifPresent(action -> action.run(new EntityCtx(owner, owner.level())));
        });
    }

    private static void push(AbstractArrow source, Entity owner, float strength) {
        Vec3 delta = source.getDeltaMovement();
        if (delta.lengthSqr() < 1.0E-6) {
            return;
        }
        Vec3 recoil = delta.normalize().scale(-strength);
        owner.setDeltaMovement(owner.getDeltaMovement().add(recoil));
        owner.hurtMarked = true;
    }

    public record Configuration(float strength, Optional<EntityAction> selfAction) {}
}
