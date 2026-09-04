package dev.muon.raven_dnd_origins.action.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;

public final class ActionBarMessageAction implements ActionType<EntityCtx, ActionBarMessageAction.Cfg> {

    public record Cfg(Component message) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                ComponentSerialization.CODEC.fieldOf("message").forGetter(Cfg::message)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        if (ctx.level().isClientSide() || !(ctx.entity() instanceof ServerPlayer player)) {
            return;
        }
        player.connection.send(new ClientboundSetActionBarTextPacket(cfg.message()));
    }
}
