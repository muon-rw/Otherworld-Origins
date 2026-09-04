package dev.muon.raven_dnd_origins.condition.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.data.Comparison;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.minecraft.world.entity.player.Player;

public final class ManaCondition implements ConditionType<EntityCtx, ManaCondition.Cfg> {
    public record Cfg(Comparison comparison, int compareTo) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                Comparison.CODEC.fieldOf("comparison").forGetter(Cfg::comparison),
                Codec.INT.fieldOf("compare_to").forGetter(Cfg::compareTo)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        int mana = ctx.raw() instanceof Player player ? (int) MagicData.getPlayerMagicData(player).getMana() : 0;
        return cfg.comparison().compare(mana, cfg.compareTo());
    }
}
