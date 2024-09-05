package dev.muon.otherworldorigins.action;

import dev.muon.otherworldorigins.OtherworldOrigins;
import io.github.edwinmindcraft.apoli.api.configuration.NoConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.BiEntityAction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;

import java.util.function.BiConsumer;

public class TameAction extends BiEntityAction<NoConfiguration> {

    public static void customTame(Entity actor, Entity target) {
        if (actor instanceof Player player && target instanceof TamableAnimal tamable) {
            if (!tamable.isTame()) {
                tamable.tame(player);
                OtherworldOrigins.LOGGER.info("Custom tame action executed: " + tamable.getClass().getSimpleName() + " tamed by " + player.getName().getString());
            } else {
                OtherworldOrigins.LOGGER.info("Custom tame action failed: " + tamable.getClass().getSimpleName() + " is already tamed");
            }
        } else {
            OtherworldOrigins.LOGGER.info("Custom tame action failed: Invalid actor or target");
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