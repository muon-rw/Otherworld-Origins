package dev.muon.otherworldorigins.condition;

import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.condition.entity.AnyOnLayerCondition;
import dev.muon.otherworldorigins.condition.entity.HasSkillCondition;
import dev.muon.otherworldorigins.condition.entity.ManaCondition;
import dev.muon.otherworldorigins.condition.entity.PlayerLevelCondition;
import io.github.apace100.apoli.power.factory.condition.ConditionFactory;
import io.github.apace100.apoli.registry.ApoliRegistries;
import io.github.apace100.calio.data.SerializableData;
import net.minecraft.core.Registry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;

public class ModEntityConditions {
    public static void register() {
        register(new ConditionFactory<>(OtherworldOrigins.loc("is_arrow"), new SerializableData(), (data, entity)
                -> entity instanceof AbstractArrow));
        register(new ConditionFactory<>(OtherworldOrigins.loc("creative_mode"), new SerializableData(), (data, entity)
                -> entity instanceof Player player && player.getAbilities().instabuild));
        register(AnyOnLayerCondition.getFactory());
        register(HasSkillCondition.getFactory());
        register(ManaCondition.getFactory());
        register(PlayerLevelCondition.getFactory());
    }

    private static void register(ConditionFactory<Entity> serializer) {
        Registry.register(ApoliRegistries.ENTITY_CONDITION, serializer.getSerializerId(), serializer);
    }
}
