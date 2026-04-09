package dev.muon.otherworldorigins.action.entity;

import dev.muon.otherworldorigins.util.ExperimentalElixirConfiguration;
import dev.muon.otherworldorigins.util.ExperimentalElixirLogic;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityAction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class ExperimentalElixirEntityAction extends EntityAction<ExperimentalElixirConfiguration.Configuration> {

    public ExperimentalElixirEntityAction() {
        super(ExperimentalElixirConfiguration.Configuration.CODEC);
    }

    @Override
    public void execute(ExperimentalElixirConfiguration.Configuration configuration, Entity entity) {
        if (!(entity instanceof Player player) || entity.level().isClientSide()) {
            return;
        }
        ExperimentalElixirLogic.brewForPlayer(player, configuration, entity.level().getRandom());
    }
}
