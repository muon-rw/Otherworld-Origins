package dev.muon.otherworldorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredEntityAction;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;

/**
 * Pushes the arrow's owner opposite the arrow's post-shoot delta movement. Called once per
 * real {@code shoot()} (multishot clones bypass {@code shoot()}, so the recoil stays per-shot
 * even when Hail of Arrows spawns extras).
 */
public class MomentumPower extends PowerFactory<MomentumPower.Configuration> {

    public MomentumPower() {
        super(Configuration.CODEC);
    }

    public static void applyRecoil(AbstractArrow source, Entity owner) {
        if (owner.level().isClientSide() || !(owner instanceof net.minecraft.world.entity.LivingEntity)) {
            return;
        }
        IPowerContainer.get(owner).ifPresent(container -> {
            for (Holder<ConfiguredPower<Configuration, MomentumPower>> holder :
                    container.getPowers(ModPowers.MOMENTUM.get())) {
                if (!holder.isBound() || !holder.value().isActive(owner)) {
                    continue;
                }
                Configuration cfg = holder.value().getConfiguration();
                push(source, owner, cfg.strength());
                ConfiguredEntityAction.execute(cfg.selfAction(), owner);
            }
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

    public record Configuration(float strength, Holder<ConfiguredEntityAction<?, ?>> selfAction) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("strength", 0.6F).forGetter(Configuration::strength),
                ConfiguredEntityAction.optional("self_action").forGetter(Configuration::selfAction)
        ).apply(instance, Configuration::new));

        @Override
        public boolean isConfigurationValid() {
            return strength >= 0F;
        }
    }
}
