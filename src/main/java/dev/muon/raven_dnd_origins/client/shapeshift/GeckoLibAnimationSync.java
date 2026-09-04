package dev.muon.raven_dnd_origins.client.shapeshift;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.animatable.GeoEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * Drives GeckoLib wild shape forms (Hybrid Aquatic's orca and any other GeoEntity form).
 * GeckoLib controllers read the entity itself instead of an externally started AnimationState:
 * swim/idle keys off delta movement plus limb swing, which the generic visual sync already mirrors
 * onto the form, while attacks key off {@link LivingEntity#swinging}. The swing is latched here so
 * that combo attacks which never restart the player's vanilla swing still animate.
 */
public class GeckoLibAnimationSync implements ShapeshiftRenderHelper.FormAnimationSync {

    private static final int SWING_TICKS = 6;

    private final Map<Integer, Integer> swingEndTick = new HashMap<>();

    public static void register() {
        ShapeshiftRenderHelper.registerFormAnimationSync(new GeckoLibAnimationSync());
    }

    @Override
    public void syncFrame(LivingEntity source, Entity form, boolean newSourceTick) {
        if (!(form instanceof GeoEntity) || !(form instanceof LivingEntity livingForm)) return;

        Integer end = swingEndTick.get(source.getId());
        if (end == null) return;

        int remaining = end - form.tickCount;
        if (remaining <= 0) {
            swingEndTick.remove(source.getId());
            return;
        }

        float progress = 1.0F - remaining / (float) SWING_TICKS;
        livingForm.swinging = true;
        livingForm.swingTime = SWING_TICKS - remaining;
        livingForm.oAttackAnim = progress;
        livingForm.attackAnim = progress;
    }

    @Override
    public void triggerAttack(LivingEntity source, Entity form, String animationKey) {
        if (!(form instanceof GeoEntity)) return;
        swingEndTick.put(source.getId(), form.tickCount + SWING_TICKS);
    }
}
