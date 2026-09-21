package dev.muon.raven_dnd_origins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.raven_dnd_origins.util.LivePerformanceTracker;
import dev.overgrown.apoli.action.BiEntityAction;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import immersive_melodies.item.InstrumentItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.Optional;

/**
 * While holding an Immersive Melodies instrument that is playing a melody or being played live,
 * heals living entities within {@code radius} every {@code interval} ticks.
 */
public class InspirationPower extends PowerType<InspirationPower.Configuration> {

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.FLOAT.fieldOf("heal_amount").forGetter(Configuration::healAmount),
            Codec.DOUBLE.fieldOf("radius").forGetter(Configuration::radius),
            Codec.INT.optionalFieldOf("interval", 20).forGetter(Configuration::interval),
            LoggedOptionalField.strict("bientity_condition", BiEntityCondition.CODEC).forGetter(Configuration::bientityCondition),
            LoggedOptionalField.of("bientity_action", BiEntityAction.CODEC).forGetter(Configuration::bientityAction)
    ).apply(instance, Configuration::new));

    @Override
    public MapCodec<Configuration> configCodec() {
        return CODEC;
    }

    @Override
    public void tick(ResourceLocation powerId, Configuration cfg, PowerContainer holder) {
        Entity owner = holder.rawOwner();
        if (!(owner instanceof LivingEntity musician) || !(owner.level() instanceof ServerLevel level)) {
            return;
        }
        int interval = Math.max(1, cfg.interval());
        int phase = Math.floorMod(powerId.hashCode() + owner.getId(), interval);
        if (owner.tickCount % interval != phase) {
            return;
        }
        if (!conditionHolds(powerId, musician, level)) {
            return;
        }
        if (!isPerforming(musician)) {
            return;
        }

        double radius = cfg.radius();
        AABB area = musician.getBoundingBox().inflate(radius, radius, radius);
        float heal = cfg.healAmount();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (!target.isAlive()) {
                continue;
            }
            BiEntityCtx ctx = new BiEntityCtx(musician, target, level);
            if (cfg.bientityCondition().isPresent() && !cfg.bientityCondition().get().test(ctx)) {
                continue;
            }
            if (heal > 0.0F) {
                target.heal(heal);
            }
            cfg.bientityAction().ifPresent(action -> action.run(ctx));
        }
    }

    private static boolean conditionHolds(ResourceLocation powerId, LivingEntity musician, ServerLevel level) {
        Power power = ApoliPowers.get(powerId);
        return power == null || power.condition().isEmpty()
                || power.condition().get().test(new EntityCtx(musician, level));
    }

    private static boolean isPerforming(LivingEntity musician) {
        boolean holdingInstrument = false;
        for (ItemStack stack : musician.getHandSlots()) {
            if (stack.getItem() instanceof InstrumentItem instrument) {
                if (instrument.isPlaying(stack)) {
                    return true;
                }
                holdingInstrument = true;
            }
        }
        if (!holdingInstrument) {
            // A note-off lost to a paused client would otherwise leave a tone held forever.
            LivePerformanceTracker.stop(musician);
            return false;
        }
        return LivePerformanceTracker.isPerforming(musician);
    }

    public record Configuration(
            float healAmount,
            double radius,
            int interval,
            Optional<BiEntityCondition> bientityCondition,
            Optional<BiEntityAction> bientityAction
    ) {}
}
