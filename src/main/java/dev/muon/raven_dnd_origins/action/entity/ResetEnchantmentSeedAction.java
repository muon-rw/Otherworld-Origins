package dev.muon.raven_dnd_origins.action.entity;

import com.mojang.serialization.MapCodec;
import dev.muon.raven_dnd_origins.util.IEnchantmentSeedResettable;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.shared.EmptyCfg;
import net.minecraft.world.entity.player.Player;

public final class ResetEnchantmentSeedAction implements ActionType<EntityCtx, EmptyCfg> {

    @Override
    public MapCodec<EmptyCfg> codec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    @Override
    public void run(EmptyCfg cfg, EntityCtx ctx) {
        if (ctx.entity() instanceof Player player && player instanceof IEnchantmentSeedResettable resettable) {
            resettable.raven_dnd_origins$resetEnchantmentSeed();
        }
    }
}
