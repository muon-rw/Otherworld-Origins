package dev.muon.otherworldorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import immersive_melodies.item.InstrumentItem;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredBiEntityAction;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredBiEntityCondition;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import io.github.edwinmindcraft.calio.api.network.CalioCodecHelper;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/**
 * While holding an Immersive Melodies instrument that is actively playing (server-side item NBT),
 * heals living entities within {@code radius} every {@code interval} ticks.
 */
public class InspirationPower extends PowerFactory<InspirationPower.Configuration> {

    public InspirationPower() {
        super(Configuration.CODEC);
        ticking();
    }

    @Override
    protected int tickInterval(Configuration configuration, Entity entity) {
        return Math.max(1, configuration.interval());
    }

    @Override
    protected void tick(Configuration configuration, Entity entity) {
        if (!(entity instanceof LivingEntity musician) || entity.level().isClientSide()) {
            return;
        }
        if (!isHoldingPlayingInstrument(musician)) {
            return;
        }

        double radius = configuration.radius();
        AABB area = musician.getBoundingBox().inflate(radius, radius, radius);
        float heal = configuration.healAmount();
        for (LivingEntity target : musician.level().getEntitiesOfClass(LivingEntity.class, area)) {
            if (!target.isAlive() || !ConfiguredBiEntityCondition.check(configuration.bientityCondition(), musician, target)) {
                continue;
            }
            if (heal > 0.0F) {
                target.heal(heal);
            }
            ConfiguredBiEntityAction.execute(configuration.bientityAction(), musician, target);
        }
    }

    private static boolean isHoldingPlayingInstrument(LivingEntity entity) {
        for (var stack : entity.getHandSlots()) {
            if (stack.getItem() instanceof InstrumentItem instrument && instrument.isPlaying(stack)) {
                return true;
            }
        }
        return false;
    }

    public record Configuration(
            float healAmount,
            double radius,
            int interval,
            Holder<ConfiguredBiEntityCondition<?, ?>> bientityCondition,
            Holder<ConfiguredBiEntityAction<?, ?>> bientityAction
    ) implements IDynamicFeatureConfiguration {

        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.fieldOf("heal_amount").forGetter(Configuration::healAmount),
                Codec.DOUBLE.fieldOf("radius").forGetter(Configuration::radius),
                CalioCodecHelper.optionalField(CalioCodecHelper.INT, "interval", 20).forGetter(Configuration::interval),
                ConfiguredBiEntityCondition.optional("bientity_condition").forGetter(Configuration::bientityCondition),
                ConfiguredBiEntityAction.optional("bientity_action").forGetter(Configuration::bientityAction)
        ).apply(instance, Configuration::new));

        @Override
        public boolean isConfigurationValid() {
            return radius >= 0.0 && interval >= 1 && healAmount >= 0.0F;
        }
    }
}
