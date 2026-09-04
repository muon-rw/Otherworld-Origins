package dev.muon.raven_dnd_origins.action.bientity;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.shared.EmptyCfg;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;

public final class TameAction implements ActionType<BiEntityCtx, EmptyCfg> {

    @Override
    public MapCodec<EmptyCfg> codec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    @Override
    public void run(EmptyCfg cfg, BiEntityCtx ctx) {
        Entity actor = ctx.actor();
        Entity target = ctx.target();

        if (!(target instanceof TamableAnimal tamable)) {
            RavenDndOrigins.LOGGER.info("Tame action failed: Target entity is not tamable - " + target.getClass().getSimpleName());
            return;
        }

        if (!(actor instanceof Player player)) {
            RavenDndOrigins.LOGGER.info("Tame action failed: Actor cannot own entities - " + actor.getClass().getSimpleName());
            return;
        }

        if (tamable.isTame()) {
            RavenDndOrigins.LOGGER.info("Tame action failed: " + tamable.getClass().getSimpleName() + " is already tamed");
        } else {
            tamable.tame(player);
        }
    }
}
