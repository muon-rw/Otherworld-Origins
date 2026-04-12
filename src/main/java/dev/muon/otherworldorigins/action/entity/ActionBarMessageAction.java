package dev.muon.otherworldorigins.action.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityAction;
import io.github.edwinmindcraft.calio.api.network.CalioCodecHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Sets the action bar via {@link ClientboundSetActionBarTextPacket} using Calio's
 * {@link CalioCodecHelper#COMPONENT_CODEC} (vanilla JSON: {@code translate}, {@code color}, etc.).
 */
public class ActionBarMessageAction extends EntityAction<ActionBarMessageAction.Configuration> {

    public ActionBarMessageAction() {
        super(Configuration.CODEC);
    }

    @Override
    public void execute(Configuration configuration, Entity entity) {
        if (entity.level().isClientSide() || !(entity instanceof ServerPlayer player)) {
            return;
        }
        player.connection.send(new ClientboundSetActionBarTextPacket(configuration.message()));
    }

    public record Configuration(Component message) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                CalioCodecHelper.COMPONENT_CODEC.fieldOf("message").forGetter(Configuration::message)
        ).apply(instance, Configuration::new));

        @Override
        public boolean isConfigurationValid() {
            return true;
        }
    }
}
