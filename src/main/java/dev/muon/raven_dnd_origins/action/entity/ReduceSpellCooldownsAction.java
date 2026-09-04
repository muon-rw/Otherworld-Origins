package dev.muon.raven_dnd_origins.action.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.raven_dnd_origins.util.spell.SpellCastUtil;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.server.level.ServerPlayer;

public final class ReduceSpellCooldownsAction implements ActionType<EntityCtx, ReduceSpellCooldownsAction.Configuration> {

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("ticks").forGetter(Configuration::ticks)
    ).apply(instance, Configuration::new));

    @Override
    public MapCodec<Configuration> codec() {
        return CODEC;
    }

    @Override
    public void run(Configuration configuration, EntityCtx ctx) {
        if (ctx.entity() instanceof ServerPlayer player && !player.level().isClientSide()) {
            SpellCastUtil.reducePlayerSpellCooldowns(player, configuration.ticks());
        }
    }

    public record Configuration(int ticks) {
    }
}
