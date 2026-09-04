package dev.muon.raven_dnd_origins.condition.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.raven_dnd_origins.power.AllowedSpellsPower;
import dev.muon.raven_dnd_origins.power.ModPowers;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * True if any of the player's {@link AllowedSpellsPower} instances contain an {@code @school_id}
 * entry matching the configured school. Used (inverted) in feat layers to hide school feats the
 * player already has access to.
 */
public final class HasSchoolAccessCondition implements ConditionType<EntityCtx, HasSchoolAccessCondition.Cfg> {
    public record Cfg(ResourceLocation school) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                IdCodecs.ID.fieldOf("school").forGetter(Cfg::school)
        ).apply(i, Cfg::new));
    }

    @Override
    public boolean test(Cfg cfg, EntityCtx ctx) {
        if (!(ctx.raw() instanceof Player player)) return false;
        String schoolEntry = "@" + cfg.school();
        List<AllowedSpellsPower.Configuration> configs = PowerLookup.active(player, ModPowers.ALLOWED_SPELLS, AllowedSpellsPower.Configuration.class);
        for (AllowedSpellsPower.Configuration config : configs) {
            if (config.entries().contains(schoolEntry)) {
                return true;
            }
        }
        return false;
    }
}
