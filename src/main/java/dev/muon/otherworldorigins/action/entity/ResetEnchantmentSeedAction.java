package dev.muon.otherworldorigins.action.entity;

import dev.muon.otherworldorigins.util.IEnchantmentSeedResettable;
import io.github.edwinmindcraft.apoli.api.configuration.NoConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityAction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class ResetEnchantmentSeedAction extends EntityAction<NoConfiguration> {

    public ResetEnchantmentSeedAction() {
        super(NoConfiguration.CODEC);
    }

    @Override
    public void execute(NoConfiguration configuration, Entity entity) {
        if (entity instanceof Player player && player instanceof IEnchantmentSeedResettable resettable) {
            resettable.otherworld$resetEnchantmentSeed();
        }
    }
}