package dev.muon.raven_dnd_origins.condition.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.raven_dnd_origins.power.AllowedSpellsPower;
import dev.muon.raven_dnd_origins.power.ModPowers;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.condition.ConditionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.origins.origin.Origin;
import dev.overgrown.origins.origin.OriginRegistry;
import dev.overgrown.origins.origin.OriginView;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * True if any of the player's {@link AllowedSpellsPower} instances contain an {@code @school_id}
 * entry matching the configured school. Used (inverted) in feat layers to hide school feats the
 * player already has access to. Also reads the powers of the player's chosen origins, since the
 * selection screen mirrors picks client-side before the server's power sync arrives.
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
        for (AllowedSpellsPower.Configuration config : PowerLookup.active(player, ModPowers.ALLOWED_SPELLS, AllowedSpellsPower.Configuration.class)) {
            if (config.entries().contains(schoolEntry)) {
                return true;
            }
        }
        return chosenOriginsGrant(player, schoolEntry, ctx);
    }

    private static boolean chosenOriginsGrant(Player player, String schoolEntry, EntityCtx ctx) {
        for (ResourceLocation originId : OriginView.chosen(player).values()) {
            Origin origin = OriginRegistry.get(originId);
            if (origin == null) continue;
            for (ResourceLocation powerId : origin.powers()) {
                Power power = ApoliPowers.get(powerId);
                if (power != null
                        && power.config() instanceof AllowedSpellsPower.Configuration config
                        && config.entries().contains(schoolEntry)
                        && power.condition().map(condition -> condition.test(ctx)).orElse(true)) {
                    return true;
                }
            }
        }
        return false;
    }
}
