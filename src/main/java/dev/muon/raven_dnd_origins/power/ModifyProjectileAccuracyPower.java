package dev.muon.raven_dnd_origins.power;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.AttributeModifier;
import dev.overgrown.apoli.data.AttributeModifierHelper;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ModifyProjectileAccuracyPower extends PowerType<ModifyProjectileAccuracyPower.Configuration> {

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            AttributeModifier.CODEC.optionalFieldOf("modifier").forGetter(Configuration::modifier),
            AttributeModifier.LIST_OR_SINGLE.optionalFieldOf("modifiers").forGetter(Configuration::modifiers),
            LoggedOptionalField.of("self_action", EntityAction.CODEC).forGetter(Configuration::selfAction)
    ).apply(instance, Configuration::new));

    @Override
    public MapCodec<Configuration> configCodec() {
        return CODEC;
    }

    public static float modify(Entity entity, float baseValue) {
        List<AttributeModifier> modifiers = new ArrayList<>();
        PowerLookup.forEach(entity, ModPowers.MODIFY_PROJECTILE_ACCURACY, Configuration.class,
                cfg -> modifiers.addAll(cfg.allModifiers()));
        return modifiers.isEmpty() ? baseValue : AttributeModifierHelper.apply(baseValue, modifiers, entity);
    }

    public static boolean hasPower(Entity entity) {
        PowerContainer container = PowerContainer.of(entity);
        return container != null && !container.powersOfType(ModPowers.MODIFY_PROJECTILE_ACCURACY).isEmpty();
    }

    public static void executeActions(Entity entity) {
        Level level = entity.level();
        PowerLookup.forEach(entity, ModPowers.MODIFY_PROJECTILE_ACCURACY, Configuration.class,
                cfg -> cfg.selfAction().ifPresent(action -> action.run(new EntityCtx(entity, level))));
    }

    public record Configuration(
            Optional<AttributeModifier> modifier,
            Optional<List<AttributeModifier>> modifiers,
            Optional<EntityAction> selfAction
    ) {
        public List<AttributeModifier> allModifiers() {
            return AttributeModifierHelper.flatten(modifier, modifiers);
        }
    }
}
