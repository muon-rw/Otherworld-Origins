package dev.muon.otherworldorigins.mixin.network;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.muon.otherworldorigins.network.ExtendedMotionPacketAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Carries the unclamped delta past the packet's ±3.9/component short encoding so entities moving
 * faster than that (e.g. Sharpshooter-boosted arrows) don't mispredict on the client and drag
 * third-party trail renderers off the shot line. The extended payload is a flag byte plus three
 * doubles, only written when any component would otherwise clamp.
 *
 * <p>Application is done by routing the {@code getXa/Ya/Za} accessors to the extended value when
 * present: the vanilla handler reads velocity through those getters, so any other mixin that hooks
 * them (e.g. via {@link ModifyReturnValue}) chains cleanly without us having to redirect
 * {@code lerpMotion} downstream.
 */
@Mixin(ClientboundSetEntityMotionPacket.class)
public abstract class ClientboundSetEntityMotionPacketMixin implements ExtendedMotionPacketAccess {

    @Unique
    private static final double CLAMP_LIMIT = 3.9;

    @Unique
    private static final double WIRE_SCALE = 8000.0;

    @Unique
    private boolean otherworldorigins$hasExtendedVelocity;
    @Unique
    private double otherworldorigins$extendedX;
    @Unique
    private double otherworldorigins$extendedY;
    @Unique
    private double otherworldorigins$extendedZ;

    @Inject(method = "<init>(ILnet/minecraft/world/phys/Vec3;)V", at = @At("TAIL"))
    private void otherworldorigins$captureExtendedVelocity(int id, Vec3 delta, CallbackInfo ci) {
        if (Math.abs(delta.x) > CLAMP_LIMIT || Math.abs(delta.y) > CLAMP_LIMIT || Math.abs(delta.z) > CLAMP_LIMIT) {
            this.otherworldorigins$hasExtendedVelocity = true;
            this.otherworldorigins$extendedX = delta.x;
            this.otherworldorigins$extendedY = delta.y;
            this.otherworldorigins$extendedZ = delta.z;
        }
    }

    @Inject(method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V", at = @At("TAIL"))
    private void otherworldorigins$readExtendedVelocity(FriendlyByteBuf buffer, CallbackInfo ci) {
        if (buffer.readableBytes() <= 0) {
            return;
        }
        this.otherworldorigins$hasExtendedVelocity = buffer.readBoolean();
        if (this.otherworldorigins$hasExtendedVelocity) {
            this.otherworldorigins$extendedX = buffer.readDouble();
            this.otherworldorigins$extendedY = buffer.readDouble();
            this.otherworldorigins$extendedZ = buffer.readDouble();
        }
    }

    /**
     * Writes the extended payload if present. Values are pulled through the public
     * {@code otherworldorigins$getExtended*()} accessors rather than the raw fields so
     * downstream mixins that hook those accessors (e.g. via {@link ModifyReturnValue}) reach the wire.
     */
    @Inject(method = "write", at = @At("TAIL"))
    private void otherworldorigins$writeExtendedVelocity(FriendlyByteBuf buffer, CallbackInfo ci) {
        boolean present = this.otherworldorigins$hasExtendedVelocity();
        buffer.writeBoolean(present);
        if (present) {
            buffer.writeDouble(this.otherworldorigins$getExtendedX());
            buffer.writeDouble(this.otherworldorigins$getExtendedY());
            buffer.writeDouble(this.otherworldorigins$getExtendedZ());
        }
    }

    @ModifyReturnValue(method = "getXa", at = @At("RETURN"))
    private int otherworldorigins$extendedGetXa(int original) {
        return this.otherworldorigins$hasExtendedVelocity()
                ? (int) (this.otherworldorigins$getExtendedX() * WIRE_SCALE)
                : original;
    }

    @ModifyReturnValue(method = "getYa", at = @At("RETURN"))
    private int otherworldorigins$extendedGetYa(int original) {
        return this.otherworldorigins$hasExtendedVelocity()
                ? (int) (this.otherworldorigins$getExtendedY() * WIRE_SCALE)
                : original;
    }

    @ModifyReturnValue(method = "getZa", at = @At("RETURN"))
    private int otherworldorigins$extendedGetZa(int original) {
        return this.otherworldorigins$hasExtendedVelocity()
                ? (int) (this.otherworldorigins$getExtendedZ() * WIRE_SCALE)
                : original;
    }

    @Override
    public boolean otherworldorigins$hasExtendedVelocity() {
        return this.otherworldorigins$hasExtendedVelocity;
    }

    @Override
    public double otherworldorigins$getExtendedX() {
        return this.otherworldorigins$extendedX;
    }

    @Override
    public double otherworldorigins$getExtendedY() {
        return this.otherworldorigins$extendedY;
    }

    @Override
    public double otherworldorigins$getExtendedZ() {
        return this.otherworldorigins$extendedZ;
    }
}
