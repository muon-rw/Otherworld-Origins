package dev.muon.otherworldorigins.action;

import dev.muon.otherworldorigins.OtherworldOrigins;
import io.github.apace100.apoli.power.factory.action.ActionFactory;
import io.github.apace100.calio.data.SerializableData;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;

public class TameAction {

    public static void action(SerializableData.Instance data, Tuple<Entity, Entity> entities) {
        Entity actor = entities.getA();
        Entity target = entities.getB();

        if (!(target instanceof TamableAnimal tamable)) {
            OtherworldOrigins.LOGGER.info("Tame action failed: Target entity is not tamable - {}", target.getClass().getSimpleName());
            return;
        }

        if (!(actor instanceof Player player)) {
            OtherworldOrigins.LOGGER.info("Tame action failed: Actor cannot own entities - {}", actor.getClass().getSimpleName());
            return;
        }

        if (tamable.isTame()) {
            OtherworldOrigins.LOGGER.info("Tame action failed: {} is already tamed", tamable.getClass().getSimpleName());
        } else {
            tamable.tame(player);
        }
    }

    public static ActionFactory<Tuple<Entity, Entity>> getFactory() {
        return new ActionFactory<>(
                OtherworldOrigins.loc("tame"),
                new SerializableData(),
                TameAction::action
        );
    }
}
