package dev.muon.otherworldorigins.condition;

import dev.muon.otherworldorigins.OtherworldOrigins;
import io.github.apace100.apoli.power.factory.condition.ConditionFactory;
import io.github.apace100.apoli.registry.ApoliRegistries;
import io.github.apace100.calio.data.SerializableData;
import net.minecraft.core.Registry;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Entity;

public class ModBientityConditions {
    public static void register() {
        register(new ConditionFactory<>(OtherworldOrigins.loc("allied"), new SerializableData(), (data, entities)
                -> entities.getA().isAlliedTo(entities.getB())));
    }

    private static void register(ConditionFactory<Tuple<Entity, Entity>> serializer) {
        Registry.register(ApoliRegistries.BIENTITY_CONDITION, serializer.getSerializerId(), serializer);
    }
}

