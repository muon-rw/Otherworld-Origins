package dev.muon.raven_dnd_origins.client.shapeshift;

import com.iafenvoy.iceandfire.entity.HippogryphEntity;
import com.iafenvoy.uranus.animation.Animation;
import com.iafenvoy.uranus.animation.IAnimatedEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * Drives Ice and Fire forms, which animate through Uranus: the fly, hover and sit poses are
 * procedural on progress fields the entity advances in its own tick, and attacks run on an
 * {@link Animation} plus tick counter that Uranus's handler would step. Fake entities are never
 * ticked, so both are advanced here once per source game tick.
 */
public class IceAndFireAnimationSync implements ShapeshiftRenderHelper.FormAnimationSync {
    private static final float PROGRESS_MAX = 20.0F;
    private final Map<Integer, Boolean> lastAttackWasScratch = new HashMap<>();

    public static void register() {
        ShapeshiftRenderHelper.registerFormAnimationSync(new IceAndFireAnimationSync());
    }

    @Override
    public void syncFrame(LivingEntity source, Entity form, boolean newSourceTick) {
        if (!(form instanceof HippogryphEntity hippogryph)) {
            return;
        }
        boolean flying = source.isFallFlying();
        boolean hovering = !flying && !source.onGround() && !source.isInWater();
        hippogryph.setFlying(flying);
        hippogryph.setHovering(hovering);
        if (!newSourceTick) {
            return;
        }
        boolean airborne = flying || hovering;
        hippogryph.flyProgress = step(hippogryph.flyProgress, airborne);
        hippogryph.hoverProgress = step(hippogryph.hoverProgress, hovering);
        hippogryph.sitProgress = step(hippogryph.sitProgress, source.isShiftKeyDown() && !airborne);
        hippogryph.airBorneCounter = airborne ? hippogryph.airBorneCounter + 1 : 0;
        advanceAnimation(hippogryph);
    }

    private static float step(float progress, boolean towardsMax) {
        return towardsMax ? Math.min(progress + 1.0F, PROGRESS_MAX) : Math.max(progress - 1.0F, 0.0F);
    }

    private static void advanceAnimation(HippogryphEntity hippogryph) {
        Animation animation = hippogryph.getAnimation();
        if (animation == null || animation == IAnimatedEntity.NO_ANIMATION) {
            return;
        }
        int tick = hippogryph.getAnimationTick();
        if (tick >= animation.getDuration()) {
            hippogryph.setAnimation(IAnimatedEntity.NO_ANIMATION);
            hippogryph.setAnimationTick(0);
        } else {
            hippogryph.setAnimationTick(tick + 1);
        }
    }

    @Override
    public void triggerAttack(LivingEntity source, Entity form, String animationKey) {
        if (!(form instanceof HippogryphEntity hippogryph)) {
            return;
        }
        boolean scratch;
        if ("scratch".equals(animationKey) || "bite".equals(animationKey)) {
            scratch = "scratch".equals(animationKey);
        } else {
            scratch = !lastAttackWasScratch.getOrDefault(source.getId(), false);
        }
        lastAttackWasScratch.put(source.getId(), scratch);
        hippogryph.setAnimation(scratch ? HippogryphEntity.ANIMATION_SCRATCH : HippogryphEntity.ANIMATION_BITE);
        hippogryph.setAnimationTick(0);
    }
}
