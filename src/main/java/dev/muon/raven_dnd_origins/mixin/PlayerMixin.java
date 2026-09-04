package dev.muon.raven_dnd_origins.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.muon.raven_dnd_origins.power.ActionOnAttackPower;
import dev.muon.raven_dnd_origins.power.JumpCooldownPower;
import dev.muon.raven_dnd_origins.util.IEnchantmentSeedResettable;
import dev.muon.raven_dnd_origins.util.JumpCooldownAccess;
import dev.muon.raven_dnd_origins.util.shapeshift.ShapeshiftCollisionHelper;
import dev.muon.raven_dnd_origins.util.shapeshift.ShapeshiftCollisionShape;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(Player.class)
public class PlayerMixin implements IEnchantmentSeedResettable, JumpCooldownAccess {

    /**
     * Vanilla player standing height in blocks; used to scale pose-specific eye heights when collision is overridden.
     */
    @Unique
    private static final float raven_dnd_origins$VANILLA_STANDING_HEIGHT = 1.8F;

    // also see ench_restrictions.PlayerMixin

    @Unique
    private int raven_dnd_origins$jumpCooldownRemaining;

    @Shadow
    protected int enchantmentSeed;

    @Override
    public int raven_dnd_origins$getJumpCooldownRemaining() {
        return this.raven_dnd_origins$jumpCooldownRemaining;
    }

    @Override
    public void raven_dnd_origins$setJumpCooldownRemaining(int ticks) {
        this.raven_dnd_origins$jumpCooldownRemaining = Math.max(0, ticks);
    }

    /**
     * {@link ActionOnAttackPower}: run after vanilla melee damage and follow-ups (knockback, sweep, enchants, durability, exhaustion).
     * <p>
     * Injection is after {@code causeFoodExhaustion(0.1F)} rather than {@code @At("TAIL")} because that call lives only inside
     * the {@code hurt}Succeeded branch: {@code TAIL} still runs when the player attempted a hit but {@code hurt} returned false
     * (immune, i-frames, etc.), and we want powers only on a successful weapon hit. NeoForge moves
     * {@code resetAttackStrengthTicker} to the end of {@code attack}, so we run before that reset where {@code TAIL} would be after.
     */
    @Inject(
            method = "attack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;causeFoodExhaustion(F)V",
                    shift = At.Shift.AFTER
            )
    )
    private void raven_dnd_origins$actionOnAttackAfterSuccessfulMelee(Entity target, CallbackInfo ci) {
        ActionOnAttackPower.afterSuccessfulPlayerMeleeHit((Player) (Object) this, target);
    }

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void raven_dnd_origins$blockJumpDuringCooldown(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (JumpCooldownPower.shouldBlockJump(self)) {
            ci.cancel();
        }
    }

    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    private void raven_dnd_origins$startJumpCooldown(CallbackInfo ci) {
        JumpCooldownPower.onSuccessfulJump((Player) (Object) this);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void raven_dnd_origins$tickJumpCooldown(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (!self.level().isClientSide() || !self.isLocalPlayer()) {
            return;
        }
        if (this.raven_dnd_origins$jumpCooldownRemaining > 0) {
            this.raven_dnd_origins$jumpCooldownRemaining--;
        }
    }

    @Override
    public void raven_dnd_origins$resetEnchantmentSeed() {
        this.enchantmentSeed = ((Player) (Object) this).getRandom().nextInt();
    }

    /**
     * {@link net.minecraft.world.entity.LivingEntity#getDimensions} short-circuits {@link Pose#SLEEPING} before it
     * reaches here and applies {@code generic.scale} after, so the shapeshift box only replaces the pose default.
     * Eye height keeps its vanilla pose ratio, rescaled to the shapeshift height.
     */
    @ModifyReturnValue(method = "getDefaultDimensions", at = @At("RETURN"))
    private EntityDimensions raven_dnd_origins$shapeshiftDimensions(EntityDimensions original) {
        Player self = (Player) (Object) this;
        ShapeshiftCollisionShape shape = ShapeshiftCollisionHelper.resolve(self);
        if (shape == null) {
            return original;
        }
        float eyeHeight = original.eyeHeight() * (shape.height() / raven_dnd_origins$VANILLA_STANDING_HEIGHT);
        return EntityDimensions.scalable(shape.width(), shape.height()).withEyeHeight(eyeHeight);
    }
}
