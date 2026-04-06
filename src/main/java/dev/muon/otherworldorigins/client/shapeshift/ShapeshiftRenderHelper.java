package dev.muon.otherworldorigins.client.shapeshift;

import com.github.alexthe666.alexsmobs.entity.*;
import dev.muon.otherworldorigins.mixin.client.EntityWaterStateAccessor;
import dev.muon.otherworldorigins.mixin.client.WalkAnimationStateAccessor;
import dev.muon.otherworldorigins.power.ShapeshiftPower;
import net.bettercombat.logic.PlayerAttackProperties;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Syncs visual state from the source player entity to the dummy shapeshift entity
 * so that the target renderer produces correct animations for position, movement,
 * limb swing, attack, hurt, and death.
 */
@OnlyIn(Dist.CLIENT)
public class ShapeshiftRenderHelper {

    private static boolean renderingShapeshiftBody = false;
    /**
     * Camera obstruction fade for the current delegated shapeshift draw (fake entity + anaconda parts).
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
    // private static final Random ANIM_RANDOM = new Random();

    public static boolean isRenderingShapeshiftBody() {
        return renderingShapeshiftBody;
    }

    public static void setRenderingShapeshiftBody(boolean rendering) {
        renderingShapeshiftBody = rendering;
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

        syncAlexsMobsState(source, target);
        dispatchAttackAnimation(source, target);
        syncCitadelAnimations(source, target);
        VanillaAnimationSync.syncAnimations(source, target);
    }

    private static void syncLivingState(LivingEntity source, LivingEntity target) {
        WalkAnimationStateAccessor targetAnim = (WalkAnimationStateAccessor) (Object) target.walkAnimation;
        WalkAnimationStateAccessor sourceAnim = (WalkAnimationStateAccessor) (Object) source.walkAnimation;
        targetAnim.setSpeedOld(sourceAnim.getSpeedOld());
        target.walkAnimation.setSpeed(source.walkAnimation.speed());
        targetAnim.setPosition(source.walkAnimation.position());

        // Player yBodyRot can be ~180° from yHeadRot while strafing / moving vs. look
        // (LivingEntity.tick "headTurn"). Mob models use (yHeadRot - yBodyRot) as head yaw in
        // setupAnim; copying player split makes many wildshape heads point backward. Align to
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
     * Drives Alex's Mobs float-progress animations on the fake entity. These entities
     * use prev/current float pairs (0-5 range, 1/tick transitions) instead of vanilla
     * AnimationState or Citadel IAnimatedEntity, so they need manual ticking.
     */
    private static void syncAlexsMobsState(Entity source, Entity target) {
        if (!(source instanceof LivingEntity livingSource)) return;

        if (target instanceof EntityBaldEagle eagle) {
            boolean flying = livingSource.isFallFlying();
            eagle.setFlying(flying);

            int entityId = source.getId();
            int prevTick = PREV_TICK_COUNT.getOrDefault(entityId, -1);
            boolean isNewTick = source.tickCount != prevTick;

            if (isNewTick) {
                PREV_TICK_COUNT.put(entityId, source.tickCount);
                float yMot = -((float) source.getDeltaMovement().y * (180.0F / (float) Math.PI));

                eagle.prevFlyProgress = eagle.flyProgress;
                eagle.prevFlapAmount = eagle.flapAmount;
                eagle.prevSwoopProgress = eagle.swoopProgress;
                eagle.prevBirdPitch = eagle.birdPitch;
                eagle.prevSitProgress = eagle.sitProgress;

                eagle.birdPitch = yMot;

                if (flying) {
                    if (eagle.flyProgress < 5.0F) eagle.flyProgress++;
                } else if (eagle.flyProgress > 0.0F) {
                    eagle.flyProgress--;
                }

                if (yMot < 0.1F) {
                    eagle.flapAmount = Math.min(-yMot * 0.2F, 1.0F);
                    if (eagle.swoopProgress > 0.0F) eagle.swoopProgress--;
                } else {
                    if (eagle.flapAmount > 0.0F) eagle.flapAmount = Math.max(0.0F, eagle.flapAmount - 0.1F);
                    eagle.swoopProgress = Math.min(eagle.swoopProgress + 1.0F, yMot * 0.2F);
                }

                eagle.sitProgress = 0;
            }
        }
    }

    /**
     * Detects when the player performs an attack and dispatches the corresponding
     * entity animation. Uses BC's combo count transition as the primary signal (it
     * increments after each {@code performAttack}), with a vanilla swing rising-edge
     * fallback for non-BC attacks. This avoids the problem where rapid BC attacks
     * don't restart the vanilla swing (swingTime &lt; half duration), causing the
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

        if (ShapeshiftAnimations.hasAttackAnimations(target)) {
            ShapeshiftAnimations.triggerAttack(target, animKey);
        }
        VanillaAnimationSync.triggerNamedAttack(entityId, target, animKey);
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

    /**
     * Ticks Citadel IAnimatedEntity animations forward once per game tick.
     */
    private static void syncCitadelAnimations(Entity source, Entity target) {
        if (!ShapeshiftAnimations.hasAttackAnimations(target)) return;

        int entityId = source.getId();
        int prevTick = PREV_TICK_COUNT.getOrDefault(entityId, -1);
        int currentTick = source.tickCount;
        if (currentTick != prevTick) {
            PREV_TICK_COUNT.put(entityId, currentTick);
            ShapeshiftAnimations.tickAnimation(target);
        }
    }
}
