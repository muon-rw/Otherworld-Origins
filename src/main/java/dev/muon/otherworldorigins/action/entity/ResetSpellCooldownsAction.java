package dev.muon.otherworldorigins.action.entity;

import dev.muon.otherworldorigins.util.spell.SpellCastUtil;
import io.github.edwinmindcraft.apoli.api.configuration.NoConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityAction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Apoli entity action: clears all Iron's Spellbooks spell cooldowns for a server player and syncs to client.
 */
public class ResetSpellCooldownsAction extends EntityAction<NoConfiguration> {

    public ResetSpellCooldownsAction() {
        super(NoConfiguration.CODEC);
    }

    @Override
    public void execute(NoConfiguration configuration, Entity entity) {
        if (!(entity instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        SpellCastUtil.clearPlayerSpellCooldowns(player);
    }
}
