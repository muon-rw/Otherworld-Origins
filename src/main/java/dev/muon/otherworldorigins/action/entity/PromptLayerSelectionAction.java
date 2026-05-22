package dev.muon.otherworldorigins.action.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.otherworldorigins.selection.SelectionSessions;
import dev.muon.otherworldorigins.selection.SessionKind;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityAction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.List;

public class PromptLayerSelectionAction extends EntityAction<PromptLayerSelectionAction.Configuration> {

    public PromptLayerSelectionAction() {
        super(Configuration.CODEC);
    }

    @Override
    public void execute(Configuration configuration, Entity entity) {
        if (entity instanceof ServerPlayer player) {
            SelectionSessions.beginCleared(player, configuration.layers(), SessionKind.POWER_PROMPT);
        }
    }

    public record Configuration(List<ResourceLocation> layers) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.listOf().fieldOf("layers").forGetter(Configuration::layers)
        ).apply(instance, Configuration::new));
    }
}
