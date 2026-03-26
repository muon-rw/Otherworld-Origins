package dev.muon.otherworldorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredBiEntityCondition;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredEntityCondition;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * When active on a player, hostile mobs treat them as non-attackable for targeting and
 * {@link LivingEntity#canAttack} checks. Optional filters: {@code mob_condition} on the
 * mob, {@code bientity_condition} with actor = power holder, target = mob.
 */
public class MobsIgnorePower extends PowerFactory<MobsIgnorePower.Configuration> {

    public MobsIgnorePower() {
        super(Configuration.CODEC);
    }

    /**
     * {@code true} if {@code mob} should not target or validate attacks against {@code player}
     * (any matching active power on the player).
     */
    public static boolean preventsMobFromTargeting(LivingEntity mob, Player player) {
        if (player == null || mob == null || player.level().isClientSide()) {
            return false;
        }
        return IPowerContainer.get(player).resolve()
                .stream()
                .flatMap(container -> container.getPowers(ModPowers.MOBS_IGNORE.get()).stream())
                .anyMatch(holder -> {
                    Configuration config = holder.value().getConfiguration();
                    return ConfiguredEntityCondition.check(config.mobCondition(), mob)
                            && ConfiguredBiEntityCondition.check(config.biEntityCondition(), player, mob);
                });
    }

    public record Configuration(
            Holder<ConfiguredEntityCondition<?, ?>> mobCondition,
            Holder<ConfiguredBiEntityCondition<?, ?>> biEntityCondition
    ) implements IDynamicFeatureConfiguration {

        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ConfiguredEntityCondition.optional("mob_condition").forGetter(Configuration::mobCondition),
                ConfiguredBiEntityCondition.optional("bientity_condition").forGetter(Configuration::biEntityCondition)
        ).apply(instance, Configuration::new));

        @Override
        public boolean isConfigurationValid() {
            return true;
        }
    }
}
