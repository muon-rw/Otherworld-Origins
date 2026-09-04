package dev.muon.raven_dnd_origins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.util.LeveledScaling;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.AttributeModifierOperation;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.Optional;
import com.mojang.serialization.DataResult;
import net.neoforged.fml.ModList;

public class LeveledAttributePower extends PowerType<LeveledAttributePower.Configuration> {

    public record Configuration(
            ResourceLocation attribute,
            AttributeModifierOperation operation,
            double valuePerLevel,
            double startingValue,
            boolean updateHealth,
            int tickRate,
            Optional<ResourceLocation> aptitude
    ) {
        public Optional<Holder<Attribute>> attributeHolder() {
            return BuiltInRegistries.ATTRIBUTE.getHolder(attribute).map(h -> (Holder<Attribute>) h);
        }

        public double valueForLevel(int level) {
            return startingValue + (level - 1) * valuePerLevel;
        }
    }

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.<Configuration>mapCodec(instance -> instance.group(
            IdCodecs.ID.fieldOf("attribute").forGetter(Configuration::attribute),
            AttributeModifierOperation.CODEC.fieldOf("operation").forGetter(Configuration::operation),
            Codec.DOUBLE.fieldOf("value_per_level").forGetter(Configuration::valuePerLevel),
            Codec.DOUBLE.fieldOf("starting_value").forGetter(Configuration::startingValue),
            Codec.BOOL.optionalFieldOf("update_health", true).forGetter(Configuration::updateHealth),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("tick_rate", 20).forGetter(Configuration::tickRate),
            IdCodecs.ID.optionalFieldOf("aptitude").forGetter(Configuration::aptitude)
    ).apply(instance, Configuration::new)).validate(LeveledAttributePower::validateAttribute);

    /**
     * An attribute from a mod that is loaded (or from vanilla) but missing from the registry is a
     * typo and fails the power loudly; one from an absent optional mod stays a silent no-op.
     */
    private static DataResult<Configuration> validateAttribute(Configuration cfg) {
        if (cfg.attributeHolder().isPresent()) {
            return DataResult.success(cfg);
        }
        String namespace = cfg.attribute().getNamespace();
        if (namespace.equals("minecraft") || ModList.get().isLoaded(namespace)) {
            return DataResult.error(() -> "Unknown attribute " + cfg.attribute());
        }
        return DataResult.success(cfg);
    }

    @Override
    public MapCodec<Configuration> configCodec() {
        return CODEC;
    }

    @Override
    public void onAdded(ResourceLocation powerId, Configuration cfg, PowerContainer holder, ResourceLocation source) {
        if (holder.isSuppressed(powerId)) return;
        apply(powerId, cfg, holder);
    }

    @Override
    public void onSuppressed(ResourceLocation powerId, Configuration cfg, PowerContainer holder) {
        remove(powerId, cfg, holder.owner());
    }

    @Override
    public void onRemoved(ResourceLocation powerId, Configuration cfg, PowerContainer holder, ResourceLocation source) {
        if (holder.hasPower(powerId)) return;
        remove(powerId, cfg, holder.owner());
    }

    @Override
    public void tick(ResourceLocation powerId, Configuration cfg, PowerContainer holder) {
        LivingEntity entity = holder.owner();
        if (entity == null || entity.tickCount % cfg.tickRate() != 0) return;
        apply(powerId, cfg, holder);
    }

    private static void apply(ResourceLocation powerId, Configuration cfg, PowerContainer holder) {
        LivingEntity entity = holder.owner();
        if (entity == null) return;
        AttributeInstance instance = instanceOn(entity, cfg);
        if (instance == null) return;

        ResourceLocation modifierId = modifierId(powerId);
        if (!conditionHolds(entity, powerId)) {
            removeFrom(entity, instance, modifierId, cfg);
            return;
        }

        double value = cfg.valueForLevel(LeveledScaling.levelForScaling(entity, cfg.aptitude()));
        AttributeModifier existing = instance.getModifier(modifierId);
        if (existing != null && existing.amount() == value) return;

        float previousMaxHealth = entity.getMaxHealth();
        float previousHealthPercent = healthPercent(entity, previousMaxHealth);
        instance.removeModifier(modifierId);
        instance.addTransientModifier(new AttributeModifier(modifierId, value, cfg.operation().vanillaOperation()));
        reconcileHealth(entity, cfg, previousMaxHealth, previousHealthPercent);
    }

    private static void remove(ResourceLocation powerId, Configuration cfg, LivingEntity entity) {
        if (entity == null) return;
        AttributeInstance instance = instanceOn(entity, cfg);
        if (instance == null) return;
        removeFrom(entity, instance, modifierId(powerId), cfg);
    }

    private static void removeFrom(LivingEntity entity, AttributeInstance instance, ResourceLocation modifierId, Configuration cfg) {
        if (instance.getModifier(modifierId) == null) return;
        float previousMaxHealth = entity.getMaxHealth();
        float previousHealthPercent = healthPercent(entity, previousMaxHealth);
        instance.removeModifier(modifierId);
        reconcileHealth(entity, cfg, previousMaxHealth, previousHealthPercent);
    }

    private static AttributeInstance instanceOn(LivingEntity entity, Configuration cfg) {
        if (entity.level().isClientSide()) return null;
        return cfg.attributeHolder().map(entity::getAttribute).orElse(null);
    }

    private static float healthPercent(LivingEntity entity, float maxHealth) {
        return maxHealth > 0 ? Mth.clamp(entity.getHealth() / maxHealth, 0.0f, 1.0f) : 1.0f;
    }

    private static void reconcileHealth(LivingEntity entity, Configuration cfg, float previousMaxHealth, float previousHealthPercent) {
        if (!cfg.updateHealth()) return;
        float newMaxHealth = entity.getMaxHealth();
        if (newMaxHealth > 0 && newMaxHealth != previousMaxHealth) {
            entity.setHealth(newMaxHealth * previousHealthPercent);
        }
    }

    private static boolean conditionHolds(LivingEntity entity, ResourceLocation powerId) {
        Power loaded = ApoliPowers.get(powerId);
        if (loaded == null || loaded.condition().isEmpty()) return true;
        if (!(entity.level() instanceof ServerLevel level)) return true;
        return loaded.condition().get().test(new EntityCtx(entity, level));
    }

    private static ResourceLocation modifierId(ResourceLocation powerId) {
        return RavenDndOrigins.loc("leveled_attribute/" + powerId.getNamespace() + "/" + powerId.getPath().replace('/', '_'));
    }
}
