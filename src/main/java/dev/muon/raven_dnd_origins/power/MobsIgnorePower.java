package dev.muon.raven_dnd_origins.power;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.BiEntityCondition;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

/**
 * When active on a player, hostile mobs treat them as non-attackable for targeting and
 * {@link LivingEntity#canAttack} checks. Optional filters: {@code mob_condition} on the
 * mob, {@code bientity_condition} with actor = power holder, target = mob.
 */
public final class MobsIgnorePower extends PowerType<MobsIgnorePower.Configuration> {

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            EntityCondition.CODEC.optionalFieldOf("mob_condition").forGetter(Configuration::mobCondition),
            BiEntityCondition.CODEC.optionalFieldOf("bientity_condition").forGetter(Configuration::biEntityCondition)
    ).apply(i, Configuration::new));

    @Override
    public MapCodec<Configuration> configCodec() {
        return CODEC;
    }

    /**
     * {@code true} if {@code mob} should not target or validate attacks against {@code player}
     * (any matching active power on the player).
     */
    public static boolean preventsMobFromTargeting(LivingEntity mob, Player player) {
        if (player == null || mob == null || player.level().isClientSide()) {
            return false;
        }
        boolean[] prevents = new boolean[]{false};
        PowerLookup.forEach(player, ModPowers.MOBS_IGNORE, Configuration.class, cfg -> {
            if (prevents[0]) return;
            if (matches(cfg, mob, player)) prevents[0] = true;
        });
        return prevents[0];
    }

    private static boolean matches(Configuration cfg, LivingEntity mob, Player player) {
        if (cfg.mobCondition().isPresent() && !cfg.mobCondition().get().test(EntityCtx.of(mob, mob.level()))) {
            return false;
        }
        return cfg.biEntityCondition().isEmpty()
                || cfg.biEntityCondition().get().test(BiEntityCtx.of(player, mob, player.level()));
    }

    public record Configuration(
            Optional<EntityCondition> mobCondition,
            Optional<BiEntityCondition> biEntityCondition
    ) {
    }
}
