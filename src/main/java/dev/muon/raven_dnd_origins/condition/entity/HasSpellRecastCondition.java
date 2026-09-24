package dev.muon.raven_dnd_origins.condition.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class HasSpellRecastCondition implements ConditionType<EntityCtx, HasSpellRecastCondition.Cfg> {
    public record Cfg(ResourceLocation spell) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                ResourceLocation.CODEC.fieldOf("spell").forGetter(Cfg::spell)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        return ctx.raw() instanceof ServerPlayer player
                && MagicData.getPlayerMagicData(player).getPlayerRecasts().hasRecastForSpell(cfg.spell().toString());
    }
}
