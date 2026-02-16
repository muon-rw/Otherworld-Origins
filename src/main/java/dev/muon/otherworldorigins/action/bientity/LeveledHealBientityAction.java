package dev.muon.otherworldorigins.action.bientity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.otherworld.leveling.LevelingUtils;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.BiEntityAction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class LeveledHealBientityAction extends BiEntityAction<LeveledHealBientityAction.Configuration> {

    public LeveledHealBientityAction() {
        super(Configuration.CODEC);
    }

    @Override
    public void execute(Configuration configuration, Entity actor, Entity target) {
        if (!(target instanceof LivingEntity livingTarget) || actor.level().isClientSide()) {
            return;
        }

        int level = actor instanceof Player player ? LevelingUtils.getPlayerLevel(player) : 1;
        float healAmount = configuration.base() + (configuration.perLevel() * level);

        livingTarget.heal(healAmount);
    }

    public record Configuration(
            float base,
            float perLevel
    ) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.fieldOf("base").forGetter(Configuration::base),
                Codec.FLOAT.fieldOf("per_level").forGetter(Configuration::perLevel)
        ).apply(instance, Configuration::new));

        @Override
        public boolean isConfigurationValid() {
            return true;
        }
    }
}
