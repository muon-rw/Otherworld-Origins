package dev.muon.raven_dnd_origins.condition.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.origins.component.PlayerOriginsAttachment;
import dev.overgrown.origins.component.PlayerOriginsImpl;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public final class AnyOnLayerCondition implements ConditionType<EntityCtx, AnyOnLayerCondition.Cfg> {
    public record Cfg(ResourceLocation layer) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                ResourceLocation.CODEC.fieldOf("layer").forGetter(Cfg::layer)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.raw() instanceof Player player)) return false;
        PlayerOriginsImpl origins = PlayerOriginsAttachment.get(player);
        return origins != null && origins.hasOrigin(cfg.layer());
    }
}
