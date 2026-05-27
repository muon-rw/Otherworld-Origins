package dev.muon.otherworldorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Stamps potions brewed by the holder with bonus duration/amplifier per effect category.
 * Applied at brewing-stand brew completion via {@code BrewingStandBlockEntityMixin} and at
 * Ars Elixirum Glass Cauldron scoop via {@code compat.ars_elixirum.ElixirScoopUpMixin}.
 */
public class ModifyBrewedPotionPower extends PowerFactory<ModifyBrewedPotionPower.Configuration> {

    public ModifyBrewedPotionPower() {
        super(Configuration.CODEC);
    }

    public static boolean appliesTo(Configuration config, MobEffectCategory category) {
        return (config.affectBeneficial && category == MobEffectCategory.BENEFICIAL)
                || (config.affectHarmful && category == MobEffectCategory.HARMFUL)
                || (config.affectNeutral && category == MobEffectCategory.NEUTRAL);
    }

    /**
     * Returns every active {@code modify_brewed_potion} configuration the entity holds. Filters
     * by {@code isActive} so JSON-level conditions on the power are respected. Empty if the
     * entity has no power container or no active configurations.
     */
    public static List<Configuration> getActiveConfigs(LivingEntity entity) {
        IPowerContainer container = ApoliAPI.getPowerContainer(entity);
        if (container == null) return List.of();
        var holders = container.getPowers(ModPowers.MODIFY_BREWED_POTION.get());
        if (holders.isEmpty()) return List.of();
        List<Configuration> result = new ArrayList<>(holders.size());
        for (var holder : holders) {
            if (holder.isBound() && holder.value().isActive(entity)) {
                result.add(holder.value().getConfiguration());
            }
        }
        return result;
    }

    public record Configuration(
            boolean affectBeneficial,
            boolean affectHarmful,
            boolean affectNeutral,
            float durationMultiplier,
            int amplifierModifier
    ) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("affect_beneficial", true).forGetter(Configuration::affectBeneficial),
                Codec.BOOL.optionalFieldOf("affect_harmful", true).forGetter(Configuration::affectHarmful),
                Codec.BOOL.optionalFieldOf("affect_neutral", true).forGetter(Configuration::affectNeutral),
                Codec.FLOAT.optionalFieldOf("duration_multiplier", 1.0f).forGetter(Configuration::durationMultiplier),
                Codec.INT.optionalFieldOf("amplifier_modifier", 0).forGetter(Configuration::amplifierModifier)
        ).apply(instance, Configuration::new));
    }
}
