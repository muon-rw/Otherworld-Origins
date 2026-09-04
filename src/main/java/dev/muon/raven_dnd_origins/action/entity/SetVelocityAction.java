package dev.muon.raven_dnd_origins.action.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.EntityCtx;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Set or add to the entity's delta movement on a per-axis basis. Absent axes are preserved.
 * Mirrors vanilla apoli {@code add_velocity}'s client/server split for player prediction.
 */
public final class SetVelocityAction implements ActionType<EntityCtx, SetVelocityAction.Cfg> {

    public record Cfg(Optional<Float> x, Optional<Float> y, Optional<Float> z, boolean set, boolean client, boolean server) {}

    @Override
    public MapCodec<Cfg> codec() {
        return RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.FLOAT.optionalFieldOf("x").forGetter(Cfg::x),
                Codec.FLOAT.optionalFieldOf("y").forGetter(Cfg::y),
                Codec.FLOAT.optionalFieldOf("z").forGetter(Cfg::z),
                Codec.BOOL.optionalFieldOf("set", true).forGetter(Cfg::set),
                Codec.BOOL.optionalFieldOf("client", true).forGetter(Cfg::client),
                Codec.BOOL.optionalFieldOf("server", true).forGetter(Cfg::server)
        ).apply(i, Cfg::new));
    }

    @Override
    public void run(Cfg cfg, EntityCtx ctx) {
        Entity entity = ctx.entity();
        if (entity instanceof Player) {
            if (ctx.level().isClientSide()) {
                if (!cfg.client()) {
                    return;
                }
            } else if (!cfg.server()) {
                return;
            }
        }

        Vec3 current = entity.getDeltaMovement();
        double newX = cfg.set()
                ? cfg.x().orElse((float) current.x).doubleValue()
                : current.x + cfg.x().orElse(0F);
        double newY = cfg.set()
                ? cfg.y().orElse((float) current.y).doubleValue()
                : current.y + cfg.y().orElse(0F);
        double newZ = cfg.set()
                ? cfg.z().orElse((float) current.z).doubleValue()
                : current.z + cfg.z().orElse(0F);
        entity.setDeltaMovement(newX, newY, newZ);
        entity.hurtMarked = true;
    }
}
