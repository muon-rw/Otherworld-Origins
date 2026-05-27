package dev.muon.otherworldorigins.mixin.compat.cataclysm;

import com.github.L_Ender.cataclysm.entity.projectile.Phantom_Arrow_Entity;
import dev.muon.otherworldorigins.mixin.AbstractArrowAccessor;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cataclysm's Phantom_Arrow_Entity overrides {@code onHitEntity} without calling super and
 * without populating {@code piercingIgnoreEntityIds}. When a power like Sharpshooter grants
 * pierce, {@link AbstractArrow#tick()}'s hit loop never sees the entity as "already pierced",
 * so it re-runs onHitEntity against the same target until the arrow self-discards. Each
 * Multishot/Hail of Arrows clone is an independent Phantom_Arrow, multiplying the effect.
 *
 * This restores vanilla pierce semantics: track each entity, refuse re-hits, discard when
 * the pierce budget is spent.
 */
@Mixin(value = Phantom_Arrow_Entity.class, remap = true)
public abstract class PhantomArrowEntityMixin {

    @Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true, remap = true)
    private void otherworldorigins$trackPierce(EntityHitResult p_37573_, CallbackInfo ci) {
        AbstractArrow self = (AbstractArrow) (Object) this;
        if (self.getPierceLevel() <= 0) {
            return;
        }
        AbstractArrowAccessor accessor = (AbstractArrowAccessor) self;
        IntOpenHashSet pierced = accessor.otherworldorigins$getPiercingIgnoreEntityIds();
        if (pierced == null) {
            pierced = new IntOpenHashSet(5);
            accessor.otherworldorigins$setPiercingIgnoreEntityIds(pierced);
        }
        Entity entity = p_37573_.getEntity();
        if (pierced.contains(entity.getId())) {
            ci.cancel();
            return;
        }
        if (pierced.size() >= self.getPierceLevel() + 1) {
            self.discard();
            ci.cancel();
            return;
        }
        pierced.add(entity.getId());
    }
}
