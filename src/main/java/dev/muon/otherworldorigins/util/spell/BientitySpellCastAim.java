package dev.muon.otherworldorigins.util.spell;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.UUID;

/**
 * While active on the server thread, {@link dev.muon.otherworldorigins.mixin.BientitySpellCastAimEntityMixin}
 * steers the caster's reported facing ({@code getLookAngle}, {@code getForward}, yaw/pitch, {@code getViewVector})
 * toward a bi-entity spell target for Iron's Spellbooks casts from
 * {@link SpellCastUtil#castSpellForPlayerWithBientityTarget} (and non-player {@code cast_spell}).
 * <p>
 * Nested casts push/pop on a stack (same caster is rare but supported).
 */
public final class BientitySpellCastAim {

    private static final ThreadLocal<Deque<Frame>> STACK = ThreadLocal.withInitial(ArrayDeque::new);

    private BientitySpellCastAim() {
    }

    /**
     * @param caster spell actor
     * @param target Apoli / cast target to aim at (eye to eye)
     */
    public static void push(LivingEntity caster, LivingEntity target) {
        STACK.get().push(Frame.fromEyeToEye(caster, target));
    }

    public static void pop() {
        Deque<Frame> deque = STACK.get();
        if (!deque.isEmpty()) {
            deque.pop();
        }
    }

    /**
     * Active aim frame for this entity when it is the current stack top's caster.
     */
    public static Optional<Frame> activeFrameFor(Entity entity) {
        if (entity.level().isClientSide) {
            return Optional.empty();
        }
        Deque<Frame> deque = STACK.get();
        if (deque.isEmpty()) {
            return Optional.empty();
        }
        Frame top = deque.peek();
        if (top.casterId().equals(entity.getUUID())) {
            return Optional.of(top);
        }
        return Optional.empty();
    }

    public record Frame(UUID casterId, Vec3 lookDirection, float xRot, float yRot) {

        static Frame fromEyeToEye(LivingEntity caster, LivingEntity target) {
            Vec3 start = caster.getEyePosition();
            Vec3 end = target.getEyePosition();
            Vec3 delta = end.subtract(start);
            if (delta.lengthSqr() < 1e-8) {
                delta = target.position().subtract(caster.position());
            }
            Vec3 look;
            if (delta.lengthSqr() < 1e-8) {
                look = new Vec3(0.0, 0.0, 1.0);
            } else {
                look = delta.normalize();
            }
            float[] rot = rotationToMatchCalculateViewVector(look);
            return new Frame(caster.getUUID(), look, rot[0], rot[1]);
        }

        /**
         * Inverse of {@link net.minecraft.world.entity.Entity#calculateViewVector(float, float)} so
         * {@link net.minecraft.world.entity.Entity#getLookAngle()} and derived yaw/pitch stay aligned.
         */
        private static float[] rotationToMatchCalculateViewVector(Vec3 n) {
            double x = n.x;
            double y = n.y;
            double z = n.z;
            float xRot = (float) (Mth.RAD_TO_DEG * Math.asin(Mth.clamp(-y, -1.0, 1.0)));
            float xzLenSq = (float) (x * x + z * z);
            float yRot;
            if (xzLenSq < 1.0E-8F) {
                yRot = 0.0F;
            } else {
                yRot = (float) (Mth.RAD_TO_DEG * Mth.atan2(-(float) x, (float) z));
            }
            return new float[]{xRot, yRot};
        }
    }
}
