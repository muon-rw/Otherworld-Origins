package dev.muon.otherworldorigins.action;

import dev.muon.otherworldorigins.OtherworldOrigins;
import io.github.apace100.apoli.power.factory.action.ActionFactory;
import io.github.apace100.apoli.registry.ApoliRegistries;
import net.minecraft.core.Registry;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Entity;

public class ModBientityActions {
    public static void register() {
        register(TameAction.getFactory());
    }

    private static void register(ActionFactory<Tuple<Entity, Entity>> serializer) {
        Registry.register(ApoliRegistries.BIENTITY_ACTION, serializer.getSerializerId(), serializer);
    }
}

