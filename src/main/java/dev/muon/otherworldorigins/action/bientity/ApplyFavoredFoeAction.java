package dev.muon.otherworldorigins.action.bientity;

import dev.muon.otherworld.leveling.LevelingUtils;
import dev.muon.otherworldorigins.effect.ModEffects;
import io.github.edwinmindcraft.apoli.api.configuration.NoConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.BiEntityAction;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class ApplyFavoredFoeAction extends BiEntityAction<NoConfiguration> {

    private static final int DURATION_TICKS = 100; // 5 seconds

    public ApplyFavoredFoeAction() {
        super(NoConfiguration.CODEC);
    }

    @Override
    public void execute(NoConfiguration configuration, Entity actor, Entity target) {
        if (!(target instanceof LivingEntity livingTarget) || actor.level().isClientSide()) {
            return;
        }

        int level = actor instanceof Player player ? LevelingUtils.getPlayerLevel(player) : 1;
        int amplifier = Math.max(0, Math.min(19, level - 1)); // clamp to 0-19 for effect levels 1-20

        livingTarget.addEffect(new MobEffectInstance(ModEffects.FAVORED_FOE.get(), DURATION_TICKS, amplifier, true, false, false));
    }
}
