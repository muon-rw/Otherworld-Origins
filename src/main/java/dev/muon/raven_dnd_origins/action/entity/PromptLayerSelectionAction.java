package dev.muon.raven_dnd_origins.action.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.raven_dnd_origins.selection.SelectionSessions;
import dev.muon.raven_dnd_origins.selection.SessionKind;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class PromptLayerSelectionAction implements ActionType<EntityCtx, PromptLayerSelectionAction.Cfg> {

    public record Cfg(List<ResourceLocation> layers) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                ResourceLocation.CODEC.listOf().fieldOf("layers").forGetter(Cfg::layers)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        if (ctx.entity() instanceof ServerPlayer player) {
            SelectionSessions.beginCleared(player, cfg.layers(), SessionKind.POWER_PROMPT);
        }
    }
}
