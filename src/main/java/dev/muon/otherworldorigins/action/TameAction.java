package dev.muon.otherworldorigins.action;

import dev.muon.otherworldorigins.OtherworldOrigins;
import io.github.edwinmindcraft.apoli.api.configuration.NoConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.BiEntityAction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;

import java.util.function.BiConsumer;

public class TameAction extends BiEntityAction<NoConfiguration> {

    public static void tame(Entity actor, Entity target) {
        if (!(target instanceof TamableAnimal tamable)) {
            OtherworldOrigins.LOGGER.info("Tame action failed: Target entity is not tamable - " + target.getClass().getSimpleName());
            return;
        }

        if (!(actor instanceof Player player)) {
            OtherworldOrigins.LOGGER.info("Tame action failed: Actor cannot own entities - " + actor.getClass().getSimpleName());
            return;
        }

        if (tamable.isTame()) {
            OtherworldOrigins.LOGGER.info("Tame action failed: " + tamable.getClass().getSimpleName() + " is already tamed");
        } else {
            tamable.tame(player);
        }
    }

    private final BiConsumer<Entity, Entity> action;

    public TameAction(BiConsumer<Entity, Entity> action) {
        super(NoConfiguration.CODEC);
        this.action = action;
    }

    @Override
    public void execute(NoConfiguration configuration, Entity actor, Entity target) {
        this.action.accept(actor, target);
    }
}