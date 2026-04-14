package dev.muon.otherworldorigins.action.bientity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import dev.muon.otherworldorigins.sound.ModSounds;
import dev.muon.otherworldorigins.util.ActionOnAttackRecursionGuard;
import io.github.edwinmindcraft.apoli.api.power.factory.BiEntityAction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class AttackAction extends BiEntityAction<AttackAction.Configuration> {

    private static final float FULL_STRENGTH_EPS = 1.0e-5f;

    public AttackAction() {
        super(Configuration.CODEC);
    }

    @Override
    public void execute(Configuration configuration, Entity actor, Entity target) {
        if (!(actor instanceof Player player) || actor.level().isClientSide()) {
            return;
        }

        actor.level().playSound(
                null,
                actor.getX(),
                actor.getY(),
                actor.getZ(),
                ModSounds.DASH.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F);

        float strength = Mth.clamp(configuration.attackStrength(), 0.0f, 1.0f);
        boolean bypass = configuration.bypassesAttackSpeed();
        boolean notFullStrength = strength < 1.0f - FULL_STRENGTH_EPS;

        if (bypass || notFullStrength) {
            int savedTicker = player.attackStrengthTicker;
            float delay = player.getCurrentItemAttackStrengthDelay();
            if (delay <= 0.0f) {
                delay = 1.0f;
            }
            if (strength >= 1.0f - FULL_STRENGTH_EPS) {
                player.attackStrengthTicker = Mth.ceil(delay);
            } else {
                player.attackStrengthTicker = Mth.floor(strength * delay);
            }
            ActionOnAttackRecursionGuard.runWithSuppressedActionOnAttack(() -> player.attack(target));
            player.attackStrengthTicker = savedTicker;
        } else {
            ActionOnAttackRecursionGuard.runWithSuppressedActionOnAttack(() -> player.attack(target));
        }
    }

    public record Configuration(
            boolean bypassesAttackSpeed,
            float attackStrength
    ) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.BOOL.optionalFieldOf("bypasses_attack_speed", false).forGetter(Configuration::bypassesAttackSpeed),
                Codec.FLOAT.optionalFieldOf("attack_strength", 1.0f).forGetter(Configuration::attackStrength)
        ).apply(instance, Configuration::new));

        @Override
        public boolean isConfigurationValid() {
            return attackStrength >= 0.0f;
        }
    }
}
