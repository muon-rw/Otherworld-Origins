package dev.muon.otherworldorigins.action.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityAction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Set or add to the entity's delta movement on a per-axis basis. Absent axes are preserved.
 * Mirrors vanilla apoli {@code add_velocity}'s client/server split for player prediction.
 */
public class SetVelocityAction extends EntityAction<SetVelocityAction.Configuration> {

    public SetVelocityAction() {
        super(Configuration.CODEC);
    }

    @Override
    public void execute(Configuration configuration, Entity entity) {
        if (entity instanceof Player) {
            if (entity.level().isClientSide()) {
                if (!configuration.client()) {
                    return;
                }
            } else if (!configuration.server()) {
                return;
            }
        }

        Vec3 current = entity.getDeltaMovement();
        double newX = configuration.set()
                ? configuration.x().orElse((float) current.x).doubleValue()
                : current.x + configuration.x().orElse(0F);
        double newY = configuration.set()
                ? configuration.y().orElse((float) current.y).doubleValue()
                : current.y + configuration.y().orElse(0F);
        double newZ = configuration.set()
                ? configuration.z().orElse((float) current.z).doubleValue()
                : current.z + configuration.z().orElse(0F);
        entity.setDeltaMovement(newX, newY, newZ);
        entity.hurtMarked = true;
    }

    public record Configuration(Optional<Float> x, Optional<Float> y, Optional<Float> z, boolean set, boolean client, boolean server) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.optionalFieldOf("x").forGetter(Configuration::x),
                Codec.FLOAT.optionalFieldOf("y").forGetter(Configuration::y),
                Codec.FLOAT.optionalFieldOf("z").forGetter(Configuration::z),
                Codec.BOOL.optionalFieldOf("set", true).forGetter(Configuration::set),
                Codec.BOOL.optionalFieldOf("client", true).forGetter(Configuration::client),
                Codec.BOOL.optionalFieldOf("server", true).forGetter(Configuration::server)
        ).apply(instance, Configuration::new));

        @Override
        public boolean isConfigurationValid() {
            return x.isPresent() || y.isPresent() || z.isPresent();
        }
    }
}
