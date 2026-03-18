package dev.muon.otherworldorigins.condition.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.otherworldorigins.power.AllowedSpellsPower;
import dev.muon.otherworldorigins.power.ModPowers;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityCondition;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Returns true if any of the player's {@link AllowedSpellsPower} instances
 * contain an {@code @school_id} entry matching the configured school.
 * Used (inverted) in feat layers to hide school feats the player already has access to.
 */
public class HasSchoolAccessCondition extends EntityCondition<HasSchoolAccessCondition.Configuration> {
    public HasSchoolAccessCondition() {
        super(Configuration.CODEC);
    }

    @Override
    public boolean check(@NotNull Configuration configuration, @NotNull Entity entity) {
        if (!(entity instanceof Player player)) return false;

        var container = IPowerContainer.get(player).resolve().orElse(null);
        if (container == null) return false;

        String schoolEntry = "@" + configuration.school();

        List<Holder<ConfiguredPower<AllowedSpellsPower.Configuration, ?>>> powers =
                (List) container.getPowers(ModPowers.ALLOWED_SPELLS.get());

        for (var holder : powers) {
            AllowedSpellsPower.Configuration config = holder.value().getConfiguration();
            if (config.entries().contains(schoolEntry)) {
                return true;
            }
        }
        return false;
    }

    public record Configuration(ResourceLocation school) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("school").forGetter(Configuration::school)
        ).apply(instance, Configuration::new));
    }
}
