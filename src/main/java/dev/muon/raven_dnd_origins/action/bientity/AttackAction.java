package dev.muon.raven_dnd_origins.action.bientity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.raven_dnd_origins.sound.ModSounds;
import dev.muon.raven_dnd_origins.util.ActionOnAttackRecursionGuard;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class AttackAction implements ActionType<BiEntityCtx, AttackAction.Cfg> {

    private static final float FULL_STRENGTH_EPS = 1.0e-5f;

    public record Cfg(
            boolean bypassesAttackSpeed,
            float attackStrength
    ) {}

    private static final MapCodec<Cfg> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.optionalFieldOf("bypasses_attack_speed", false).forGetter(Cfg::bypassesAttackSpeed),
            Codec.FLOAT.optionalFieldOf("attack_strength", 1.0f).forGetter(Cfg::attackStrength)
    ).apply(i, Cfg::new));

    @Override
    public MapCodec<Cfg> codec() {
        return CODEC;
    }

    @Override
    public void run(Cfg cfg, BiEntityCtx ctx) {
        if (!(ctx.actor() instanceof Player player) || ctx.level().isClientSide()) return;
        Entity target = ctx.target();
        if (target == null) return;

        ctx.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                ModSounds.DASH.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F);

        float strength = Mth.clamp(cfg.attackStrength(), 0.0f, 1.0f);
        boolean bypass = cfg.bypassesAttackSpeed();
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
}
