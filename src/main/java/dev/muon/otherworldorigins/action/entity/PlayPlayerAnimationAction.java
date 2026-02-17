package dev.muon.otherworldorigins.action.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.network.PlayPlayerAnimationPacket;
import io.github.apace100.calio.data.SerializableDataTypes;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityAction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

public class PlayPlayerAnimationAction extends EntityAction<PlayPlayerAnimationAction.Configuration> {

    public PlayPlayerAnimationAction() {
        super(Configuration.CODEC);
    }

    @Override
    public void execute(Configuration configuration, Entity entity) {
        if (!(entity instanceof Player player) || entity.level().isClientSide()) {
            return;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            OtherworldOrigins.CHANNEL.send(
                    PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> serverPlayer),
                    new PlayPlayerAnimationPacket(serverPlayer.getUUID(), configuration.animation())
            );
        }
    }

    public record Configuration(ResourceLocation animation) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                SerializableDataTypes.IDENTIFIER.fieldOf("animation").forGetter(Configuration::animation)
        ).apply(instance, Configuration::new));

        @Override
        public boolean isConfigurationValid() {
            return true;
        }
    }
}
