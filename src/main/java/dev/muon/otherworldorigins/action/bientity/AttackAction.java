package dev.muon.otherworldorigins.action.bientity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.BiEntityAction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class AttackAction extends BiEntityAction<AttackAction.Configuration> {

    public AttackAction() {
        super(Configuration.CODEC);
    }

    @Override
    public void execute(Configuration configuration, Entity actor, Entity target) {
        if (!(actor instanceof Player player) || actor.level().isClientSide()) {
            return;
        }

        if (configuration.bypassesAttackSpeed()) {
            int savedTicker = player.attackStrengthTicker;
            player.attackStrengthTicker = 100;
            player.attack(target);
            player.attackStrengthTicker = savedTicker;
        } else {
            player.attack(target);
        }
    }

    public record Configuration(
            boolean bypassesAttackSpeed
    ) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("bypasses_attack_speed", false).forGetter(Configuration::bypassesAttackSpeed)
        ).apply(instance, Configuration::new));

        @Override
        public boolean isConfigurationValid() {
            return true;
        }
    }
}
