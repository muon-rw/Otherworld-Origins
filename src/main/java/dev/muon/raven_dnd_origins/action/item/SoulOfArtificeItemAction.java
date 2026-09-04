package dev.muon.raven_dnd_origins.action.item;

import com.mojang.serialization.MapCodec;
import dev.muon.raven_dnd_origins.util.SoulOfArtificeLogic;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.ItemCtx;
import dev.overgrown.apoli.shared.EmptyCfg;

/**
 * Applies one extra Apothic affix chosen from pools allowed by the item's current rarity.
 * Re-running removes the previous bonus affix (if recorded) and rolls a replacement.
 */
public final class SoulOfArtificeItemAction implements ActionType<ItemCtx, EmptyCfg> {

    @Override
    public MapCodec<EmptyCfg> codec() {
        return MapCodec.unit(EmptyCfg.INSTANCE);
    }

    @Override
    public void run(EmptyCfg cfg, ItemCtx ctx) {
        SoulOfArtificeLogic.applyOnItem(ctx.level(), ctx.stack());
    }
}
