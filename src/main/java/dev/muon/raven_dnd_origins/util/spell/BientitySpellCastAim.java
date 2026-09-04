package dev.muon.raven_dnd_origins.util.spell;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

/**
 * While active on the server thread, {@link dev.muon.raven_dnd_origins.mixin.BientitySpellCastAimEntityMixin}
 * steers the caster's reported facing ({@code getLookAngle}, {@code getForward}, yaw/pitch, {@code getViewVector})
 * toward a bi-entity spell target for Iron's Spellbooks casts from
 * {@link SpellCastUtil#castSpellForPlayerWithBientityTarget} (and non-player {@code cast_spell}).
 * <p>
 * Nested casts push/pop on a stack. The mixin's hot path uses {@link #activeFrameOrNull} so that when no cast is
 * active anywhere on the server, a single volatile read short-circuits the per-entity ThreadLocal lookup.
 */
public final class BientitySpellCastAim {

    private static final ThreadLocal<Deque<Frame>> STACK = ThreadLocal.withInitial(ArrayDeque::new);
    // Counter across all threads. push/pop happen on the server thread; the mixin reads on any thread.
    private static volatile int activeDepth = 0;

    private BientitySpellCastAim() {
    }

    /**
     * @param caster spell actor
     * @param target Apoli / cast target to aim at (eye to eye)
     */
    public static void push(LivingEntity caster, LivingEntity target) {
        STACK.get().push(Frame.fromEyeToEye(caster, target));
        activeDepth++;
    }

    public static void pop() {
        Deque<Frame> deque = STACK.get();
        if (!deque.isEmpty()) {
            deque.pop();
            activeDepth--;
        }
    }

    @Nullable
    public static Frame activeFrameOrNull(Entity entity) {
        if (activeDepth == 0) {
            return null;
        }
        if (entity.level().isClientSide()) {
            return null;
        }
        Deque<Frame> deque = STACK.get();
        if (deque.isEmpty()) {
            return null;
        }
        Frame top = deque.peek();
        return top.casterId().equals(entity.getUUID()) ? top : null;
    }

    public record Frame(UUID casterId, Vec3 lookDirection, float xRot, float yRot, Vec2 rotationVector) {

        static Frame fromEyeToEye(LivingEntity caster, LivingEntity target) {
            Vec3 start = caster.getEyePosition();
            Vec3 end = target.getEyePosition();
            Vec3 delta = end.subtract(start);
            if (delta.lengthSqr() < 1e-8) {
                delta = target.position().subtract(caster.position());
            }
            Vec3 look = delta.lengthSqr() < 1e-8 ? new Vec3(0.0, 0.0, 1.0) : delta.normalize();
            float xRot = pitchFromDirection(look);
            float yRot = yawFromDirection(look);
            return new Frame(caster.getUUID(), look, xRot, yRot, new Vec2(xRot, yRot));
        }

        // Inverse of Entity.calculateViewVector so getLookAngle() and derived yaw/pitch stay aligned.
        private static float pitchFromDirection(Vec3 n) {
            return (float) (Mth.RAD_TO_DEG * Math.asin(Mth.clamp(-n.y, -1.0, 1.0)));
        }

        private static float yawFromDirection(Vec3 n) {
            float xzLenSq = (float) (n.x * n.x + n.z * n.z);
            return xzLenSq < 1.0E-8F ? 0.0F : (float) (Mth.RAD_TO_DEG * Mth.atan2(-(float) n.x, (float) n.z));
        }
    }
}
