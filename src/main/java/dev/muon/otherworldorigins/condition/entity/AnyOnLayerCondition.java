package dev.muon.otherworldorigins.condition.entity;

import dev.muon.otherworldorigins.OtherworldOrigins;
import io.github.apace100.apoli.power.factory.condition.ConditionFactory;
import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.calio.data.SerializableDataTypes;
import io.github.apace100.origins.registry.ModComponents;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.origin.OriginLayers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class AnyOnLayerCondition {

    public static boolean condition(SerializableData.Instance data, Entity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        
        ResourceLocation layerId = data.getId("layer");
        OriginLayer layer = OriginLayers.getLayer(layerId);
        
        OriginComponent originComponent = ModComponents.ORIGIN.maybeGet(player).orElse(null);
        if (originComponent == null) {
            return false;
        }
        
        return originComponent.hasOrigin(layer);
    }

    public static ConditionFactory<Entity> getFactory() {
        return new ConditionFactory<>(
                OtherworldOrigins.loc("any_on_layer"),
                new SerializableData()
                        .add("layer", SerializableDataTypes.IDENTIFIER),
                AnyOnLayerCondition::condition
        );
    }
}
