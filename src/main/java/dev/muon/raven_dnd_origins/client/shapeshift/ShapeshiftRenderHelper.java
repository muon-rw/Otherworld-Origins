package dev.muon.raven_dnd_origins.client.shapeshift;

import dev.muon.raven_dnd_origins.mixin.client.EntityWaterStateAccessor;
import dev.muon.raven_dnd_origins.mixin.client.WalkAnimationStateAccessor;
import dev.muon.raven_dnd_origins.power.ShapeshiftPower;
import net.bettercombat.logic.PlayerAttackProperties;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Syncs visual state from the source player entity to the dummy shapeshift entity
 * so that the target renderer produces correct animations for position, movement,
 * limb swing, attack, hurt, and death.
 */
public class ShapeshiftRenderHelper {

    /**
     * Extension point for animation systems that vanilla {@link net.minecraft.world.entity.AnimationState}
     * reflection cannot drive (GeckoLib controllers, mod-specific prev/current float pairs).
     * Register one per mod during client setup. Every registered sync sees every wildshape form,
     * so each implementation must first check whether the form entity is one it owns.
     */
    public interface FormAnimationSync {
        void syncFrame(LivingEntity source, Entity form, boolean newSourceTick);

        void triggerAttack(LivingEntity source, Entity form, String animationKey);
    }

    private static final List<FormAnimationSync> FORM_ANIMATION_SYNCS = new CopyOnWriteArrayList<>();

    private static boolean renderingShapeshiftBody = false;
    private static boolean renderingLevel = false;
    /**
     * Camera obstruction fade for the current delegated shapeshift draw.
     * Mirrors shoulder-surfing's per-vertex alpha when the camera sits inside the wildshape bounds.
     */
    private static float shapeshiftBodyObstructionAlpha = 1.0F;
    /**
     * When true, {@link RenderType} factory methods like {@code entityCutoutNoCull} are redirected
     * to translucent equivalents so that per-vertex alpha from the obstruction fade is actually
     * blended by the GPU instead of being silently ignored by the opaque pipeline.
     */
    private static boolean useTranslucentRenderTypes = false;
    private static final Map<Integer, Boolean> PREV_SWINGING = new HashMap<>();
    private static final Map<Integer, Integer> PREV_COMBO_COUNT = new HashMap<>();
    private static final Map<Integer, Integer> PREV_TICK_COUNT = new HashMap<>();

    public static void registerFormAnimationSync(FormAnimationSync sync) {
        FORM_ANIMATION_SYNCS.add(sync);
    }

    public static boolean isRenderingShapeshiftBody() {
        return renderingShapeshiftBody;
    }

    public static void setRenderingShapeshiftBody(boolean rendering) {
        renderingShapeshiftBody = rendering;
    }

    public static boolean isRenderingLevel() {
        return renderingLevel;
    }

    public static void setRenderingLevel(boolean rendering) {
        renderingLevel = rendering;
    }

    public static float getShapeshiftBodyObstructionAlpha() {
        return shapeshiftBodyObstructionAlpha;
    }

    public static void setShapeshiftBodyObstructionAlpha(float alpha) {
        shapeshiftBodyObstructionAlpha = Mth.clamp(alpha, 0.0F, 1.0F);
    }

    public static boolean shouldUseTranslucentRenderTypes() {
        return useTranslucentRenderTypes;
    }

    public static void setUseTranslucentRenderTypes(boolean translucent) {
        useTranslucentRenderTypes = translucent;
    }

    public static void clearTracking(int entityId) {
        PREV_SWINGING.remove(entityId);
        PREV_COMBO_COUNT.remove(entityId);
        PREV_TICK_COUNT.remove(entityId);
        VanillaAnimationSync.evict(entityId);
    }

    public static void clearAllTracking() {
        PREV_SWINGING.clear();
        PREV_COMBO_COUNT.clear();
        PREV_TICK_COUNT.clear();
        VanillaAnimationSync.clearAll();
    }

    public static void syncVisualState(Entity source, Entity target) {
        target.setPosRaw(source.position().x, source.position().y, source.position().z);

        if (target instanceof EnderDragon) {
            target.setYRot(source.getYRot() + 180.0F);
        } else {
            target.setYRot(source.getYRot());
        }

        target.yRotO = source.yRotO;

        if (target instanceof Phantom) {
            target.setXRot(-source.getXRot());
            target.xRotO = -source.xRotO;
        } else if (!(target instanceof Shulker)) {
            target.setXRot(source.getXRot());
            target.xRotO = source.xRotO;
        }

        target.xo = source.xo;
        target.yo = source.yo;
        target.zo = source.zo;

        target.tickCount = source.tickCount;
        target.setOnGround(source.onGround());
        target.setDeltaMovement(source.getDeltaMovement());
        target.setShiftKeyDown(source.isShiftKeyDown());
        target.setSprinting(source.isSprinting());
        target.setSwimming(source.isSwimming());
        target.setPose(source.getPose());
        target.setSharedFlagOnFire(source.isOnFire());
        ((EntityWaterStateAccessor) target).setWasTouchingWater(source.isInWater());

        if (source instanceof LivingEntity livingSource && target instanceof LivingEntity livingTarget) {
            syncLivingState(livingSource, livingTarget);
        }

        if (source instanceof LivingEntity livingSource && target instanceof Mob mobTarget) {
            mobTarget.setAggressive(livingSource.isUsingItem());
        }

        boolean newSourceTick = consumeNewSourceTick(source);
        dispatchAttackAnimation(source, target);
        VanillaAnimationSync.syncAnimations(source, target);

        if (!FORM_ANIMATION_SYNCS.isEmpty() && source instanceof LivingEntity livingSource) {
            for (FormAnimationSync sync : FORM_ANIMATION_SYNCS) {
                sync.syncFrame(livingSource, target, newSourceTick);
            }
        }
    }

    /** True once per source game tick; rendering runs more often than ticking. */
    private static boolean consumeNewSourceTick(Entity source) {
        int entityId = source.getId();
        Integer prev = PREV_TICK_COUNT.get(entityId);
        if (prev != null && prev == source.tickCount) return false;
        PREV_TICK_COUNT.put(entityId, source.tickCount);
        return true;
    }

    private static void syncLivingState(LivingEntity source, LivingEntity target) {
        WalkAnimationStateAccessor targetAnim = (WalkAnimationStateAccessor) (Object) target.walkAnimation;
        WalkAnimationStateAccessor sourceAnim = (WalkAnimationStateAccessor) (Object) source.walkAnimation;
        targetAnim.setSpeedOld(sourceAnim.getSpeedOld());
        target.walkAnimation.setSpeed(source.walkAnimation.speed());
        targetAnim.setPosition(source.walkAnimation.position());

        // Player yBodyRot can be ~180 degrees from yHeadRot while strafing / moving vs. look
        // (LivingEntity.tick "headTurn"). Mob models use (yHeadRot - yBodyRot) as head yaw in
        // setupAnim; copying the player split makes many wildshape heads point backward. Align to
        // the dummy's entity yaw (after syncVisualState special cases like EnderDragon).
        float entityYaw = target.getYRot();
        float entityYawO = target.yRotO;
        target.yBodyRot = entityYaw;
        target.yBodyRotO = entityYawO;
        target.yHeadRot = entityYaw;
        target.yHeadRotO = entityYawO;

        target.swinging = source.swinging;
        target.swingTime = source.swingTime;
        target.oAttackAnim = source.oAttackAnim;
        target.attackAnim = source.attackAnim;
        target.swingingArm = source.swingingArm;

        target.hurtTime = source.hurtTime;
        target.hurtDuration = source.hurtDuration;
        target.deathTime = source.deathTime;
        target.invulnerableTime = source.invulnerableTime;

        if (target instanceof Bat bat) {
            bat.setResting(false);
        }
    }

    /**
     * Detects when the player performs an attack and dispatches the corresponding
     * entity animation. Uses BC's combo count transition as the primary signal (it
     * increments after each {@code performAttack}), with a vanilla swing rising-edge
     * fallback for non-BC attacks. This avoids the problem where rapid BC attacks
     * do not restart the vanilla swing (swingTime &lt; half duration), causing the
     * rising-edge detector to miss subsequent hits.
     */
    private static void dispatchAttackAnimation(Entity source, Entity target) {
        if (!(source instanceof LivingEntity livingSource)) return;

        int entityId = source.getId();
        boolean attacked = false;

        if (source instanceof Player player) {
            int combo = ((PlayerAttackProperties) player).getComboCount();
            int prevCombo = PREV_COMBO_COUNT.getOrDefault(entityId, combo);
            PREV_COMBO_COUNT.put(entityId, combo);
            if (combo != prevCombo) {
                attacked = true;
            }
        }

        if (!attacked) {
            boolean wasSwinging = PREV_SWINGING.getOrDefault(entityId, false);
            boolean nowSwinging = livingSource.swinging;
            PREV_SWINGING.put(entityId, nowSwinging);
            if (nowSwinging && !wasSwinging) {
                attacked = true;
            }
        } else {
            PREV_SWINGING.put(entityId, livingSource.swinging);
        }

        if (!attacked) return;

        String animKey = resolveAnimationKey(source);
        VanillaAnimationSync.triggerNamedAttack(entityId, target, animKey);
        for (FormAnimationSync sync : FORM_ANIMATION_SYNCS) {
            sync.triggerAttack(livingSource, target, animKey);
        }
    }

    private static String resolveAnimationKey(Entity source) {
        if (!(source instanceof Player player)) return "";

        ShapeshiftPower.Configuration config = ShapeshiftPower.getActiveShapeshiftConfig(player);
        if (config == null) return "";

        List<ShapeshiftPower.ShapeshiftAttack> attacks = config.attacks();
        if (attacks.isEmpty()) return "";

        int comboCount = ((PlayerAttackProperties) player).getComboCount();
        int attackIndex = ((comboCount - 1) % attacks.size() + attacks.size()) % attacks.size();
        return attacks.get(attackIndex).animation();
    }
}
