package dev.muon.otherworldorigins.client.shapeshift;

import com.github.alexthe666.alexsmobs.entity.*;
import dev.muon.otherworldorigins.mixin.client.WalkAnimationStateAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Syncs visual state from the source player entity to the dummy shapeshift entity
 * so that the target renderer produces correct animations for position, movement,
 * limb swing, attack, hurt, and death.
 */
@OnlyIn(Dist.CLIENT)
public class ShapeshiftRenderHelper {

    private static boolean renderingShapeshiftBody = false;
    private static final Map<Integer, Boolean> PREV_SWINGING = new HashMap<>();
    private static final Map<Integer, Integer> PREV_TICK_COUNT = new HashMap<>();
    private static final Random ANIM_RANDOM = new Random();

    public static boolean isRenderingShapeshiftBody() {
        return renderingShapeshiftBody;
    }

    public static void setRenderingShapeshiftBody(boolean rendering) {
        renderingShapeshiftBody = rendering;
    }

    public static void clearTracking(int entityId) {
        PREV_SWINGING.remove(entityId);
        PREV_TICK_COUNT.remove(entityId);
        VanillaAnimationSync.evict(entityId);
    }

    public static void clearAllTracking() {
        PREV_SWINGING.clear();
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

        if (source instanceof LivingEntity livingSource && target instanceof LivingEntity livingTarget) {
            syncLivingState(livingSource, livingTarget);
        }

        if (source instanceof LivingEntity livingSource && target instanceof Mob mobTarget) {
            mobTarget.setAggressive(livingSource.isUsingItem());
        }

        syncAlexsMobsState(source, target);
        syncCitadelAnimations(source, target);
        VanillaAnimationSync.syncAnimations(source, target);
    }

    private static void syncLivingState(LivingEntity source, LivingEntity target) {
        WalkAnimationStateAccessor targetAnim = (WalkAnimationStateAccessor) (Object) target.walkAnimation;
        WalkAnimationStateAccessor sourceAnim = (WalkAnimationStateAccessor) (Object) source.walkAnimation;
        targetAnim.setSpeedOld(sourceAnim.getSpeedOld());
        target.walkAnimation.setSpeed(source.walkAnimation.speed());
        targetAnim.setPosition(source.walkAnimation.position());

        target.yBodyRot = source.yBodyRot;
        target.yBodyRotO = source.yBodyRotO;
        target.yHeadRot = source.yHeadRot;
        target.yHeadRotO = source.yHeadRotO;

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
     * Handles Citadel IAnimatedEntity animations on the fake entity:
     * - Triggers a random attack animation when the player starts swinging
     * - Ticks the animation forward once per game tick (detected via tickCount change)
     */
    private static void syncCitadelAnimations(Entity source, Entity target) {
        if (!ShapeshiftAnimations.hasAttackAnimations(target)) return;
        if (!(source instanceof LivingEntity livingSource)) return;

        int entityId = source.getId();

        boolean wasSwinging = PREV_SWINGING.getOrDefault(entityId, false);
        boolean nowSwinging = livingSource.swinging;
        PREV_SWINGING.put(entityId, nowSwinging);

        if (nowSwinging && !wasSwinging) {
            ShapeshiftAnimations.triggerRandomAttack(target, ANIM_RANDOM);
        }

        int prevTick = PREV_TICK_COUNT.getOrDefault(entityId, -1);
        int currentTick = source.tickCount;
        if (currentTick != prevTick) {
            PREV_TICK_COUNT.put(entityId, currentTick);
            ShapeshiftAnimations.tickAnimation(target);
        }
    }
}
