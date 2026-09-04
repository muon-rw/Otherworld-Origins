package dev.muon.raven_dnd_origins.action.bientity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.raven_dnd_origins.util.LeveledScaling;
import dev.overgrown.apoli.action.ActionType;
import dev.overgrown.apoli.condition.context.BiEntityCtx;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class LeveledDamageBientityAction implements ActionType<BiEntityCtx, LeveledDamageBientityAction.Cfg> {

    public record Cfg(
            float base,
            float perLevel,
            Optional<ResourceLocation> aptitude,
            Optional<ResourceLocation> damageType
    ) {}

    private static final MapCodec<Cfg> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.fieldOf("base").forGetter(Cfg::base),
            Codec.FLOAT.fieldOf("per_level").forGetter(Cfg::perLevel),
            ResourceLocation.CODEC.optionalFieldOf("aptitude").forGetter(Cfg::aptitude),
            ResourceLocation.CODEC.optionalFieldOf("damage_type").forGetter(Cfg::damageType)
    ).apply(i, Cfg::new));

    @Override
    public MapCodec<Cfg> codec() {
        return CODEC;
    }

    @Override
    public void run(Cfg cfg, BiEntityCtx ctx) {
        LivingEntity actor = ctx.livingActor();
        LivingEntity target = ctx.livingTarget();
        if (actor == null || target == null || ctx.level().isClientSide()) return;

        int level = LeveledScaling.levelForScaling(actor, cfg.aptitude());
        float damage = cfg.base() + (cfg.perLevel() * level);
        if (damage <= 0) return;

        DamageSource source = resolveDamageSource(target, actor, cfg.damageType());
        target.hurt(source, damage);
    }

    private static DamageSource resolveDamageSource(LivingEntity target, LivingEntity attacker, Optional<ResourceLocation> damageTypeId) {
        if (damageTypeId.isPresent()) {
            ResourceKey<DamageType> key = ResourceKey.create(Registries.DAMAGE_TYPE, damageTypeId.get());
            Optional<Holder.Reference<DamageType>> holder = target.level()
                    .registryAccess()
                    .registryOrThrow(Registries.DAMAGE_TYPE)
                    .getHolder(key);
            if (holder.isPresent()) {
                return new DamageSource(holder.get(), attacker);
            }
        }
        return defaultMeleeDamageSource(target, attacker);
    }

    private static DamageSource defaultMeleeDamageSource(LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player player) {
            return target.damageSources().playerAttack(player);
        }
        return target.damageSources().mobAttack(attacker);
    }
}
