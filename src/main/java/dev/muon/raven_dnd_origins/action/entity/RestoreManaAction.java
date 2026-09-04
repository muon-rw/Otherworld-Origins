package dev.muon.raven_dnd_origins.action.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.raven_dnd_origins.util.spell.SpellCastUtil;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.server.level.ServerPlayer;

public final class RestoreManaAction implements ActionType<EntityCtx, RestoreManaAction.Configuration> {

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.FLOAT.fieldOf("amount").forGetter(Configuration::amount)
    ).apply(instance, Configuration::new));

    @Override
    public MapCodec<Configuration> codec() {
        return CODEC;
    }

    @Override
    public void run(Configuration configuration, EntityCtx ctx) {
        if (ctx.entity() instanceof ServerPlayer player && !player.level().isClientSide()) {
            SpellCastUtil.restorePlayerMana(player, configuration.amount());
        }
    }

    public record Configuration(float amount) {
    }
}
