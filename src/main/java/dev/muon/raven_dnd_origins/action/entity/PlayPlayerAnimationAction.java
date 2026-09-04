package dev.muon.raven_dnd_origins.action.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.raven_dnd_origins.network.PlayPlayerAnimationPacket;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PlayPlayerAnimationAction implements ActionType<EntityCtx, PlayPlayerAnimationAction.Cfg> {

    public record Cfg(ResourceLocation animation) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                ResourceLocation.CODEC.fieldOf("animation").forGetter(Cfg::animation)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        if (ctx.level().isClientSide() || !(ctx.entity() instanceof Player player)) {
            return;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                    serverPlayer, new PlayPlayerAnimationPacket(serverPlayer.getUUID(), cfg.animation()));
        }
    }
}
