package dev.muon.otherworldorigins.action.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.otherworldorigins.compat.elixirum.ArsElixirHelper;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityAction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Reveals N random unknown Ars Elixirum ingredients on the player's profile. Themed as the
 * Alchemist's intuition — they spontaneously recall how a new reagent behaves on level-up.
 */
public class DiscoverIngredientsAction extends EntityAction<DiscoverIngredientsAction.Configuration> {

    public DiscoverIngredientsAction() {
        super(Configuration.CODEC);
    }

    @Override
    public void execute(Configuration configuration, Entity entity) {
        if (!(entity instanceof Player player) || entity.level().isClientSide()) return;
        ArsElixirHelper.discoverRandomIngredients(player, configuration.count(), entity.level().getRandom());
    }

    public record Configuration(int count) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("count", 1).forGetter(Configuration::count)
        ).apply(instance, Configuration::new));
    }
}
