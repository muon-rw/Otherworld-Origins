package dev.muon.raven_dnd_origins.mixin.network;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.muon.raven_dnd_origins.network.ExtendedMotionPacketAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Carries the unclamped delta past the packet's short encoding (3.9 per component) so entities moving
 * faster than that (e.g. Sharpshooter-boosted arrows) do not mispredict on the client and drag
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
    private boolean raven_dnd_origins$hasExtendedVelocity;
    @Unique
    private double raven_dnd_origins$extendedX;
    @Unique
    private double raven_dnd_origins$extendedY;
    @Unique
    private double raven_dnd_origins$extendedZ;

    @Inject(method = "<init>(ILnet/minecraft/world/phys/Vec3;)V", at = @At("TAIL"))
    private void raven_dnd_origins$captureExtendedVelocity(int id, Vec3 delta, CallbackInfo ci) {
        if (Math.abs(delta.x) > CLAMP_LIMIT || Math.abs(delta.y) > CLAMP_LIMIT || Math.abs(delta.z) > CLAMP_LIMIT) {
            this.raven_dnd_origins$hasExtendedVelocity = true;
            this.raven_dnd_origins$extendedX = delta.x;
            this.raven_dnd_origins$extendedY = delta.y;
            this.raven_dnd_origins$extendedZ = delta.z;
        }
    }

    @Inject(method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V", at = @At("TAIL"))
    private void raven_dnd_origins$readExtendedVelocity(FriendlyByteBuf buffer, CallbackInfo ci) {
        if (buffer.readableBytes() <= 0) {
            return;
        }
        this.raven_dnd_origins$hasExtendedVelocity = buffer.readBoolean();
        if (this.raven_dnd_origins$hasExtendedVelocity) {
            this.raven_dnd_origins$extendedX = buffer.readDouble();
            this.raven_dnd_origins$extendedY = buffer.readDouble();
            this.raven_dnd_origins$extendedZ = buffer.readDouble();
        }
    }

    /**
     * Values are pulled through the public {@code raven_dnd_origins$getExtended*()} accessors rather
     * than the raw fields so downstream mixins that hook those accessors reach the wire.
     */
    @Inject(method = "write", at = @At("TAIL"))
    private void raven_dnd_origins$writeExtendedVelocity(FriendlyByteBuf buffer, CallbackInfo ci) {
        boolean present = this.raven_dnd_origins$hasExtendedVelocity();
        buffer.writeBoolean(present);
        if (present) {
            buffer.writeDouble(this.raven_dnd_origins$getExtendedX());
            buffer.writeDouble(this.raven_dnd_origins$getExtendedY());
            buffer.writeDouble(this.raven_dnd_origins$getExtendedZ());
        }
    }

    @ModifyReturnValue(method = "getXa", at = @At("RETURN"))
    private double raven_dnd_origins$extendedGetXa(double original) {
        return this.raven_dnd_origins$hasExtendedVelocity() ? this.raven_dnd_origins$getExtendedX() : original;
    }

    @ModifyReturnValue(method = "getYa", at = @At("RETURN"))
    private double raven_dnd_origins$extendedGetYa(double original) {
        return this.raven_dnd_origins$hasExtendedVelocity() ? this.raven_dnd_origins$getExtendedY() : original;
    }

    @ModifyReturnValue(method = "getZa", at = @At("RETURN"))
    private double raven_dnd_origins$extendedGetZa(double original) {
        return this.raven_dnd_origins$hasExtendedVelocity() ? this.raven_dnd_origins$getExtendedZ() : original;
    }

    @Override
    public boolean raven_dnd_origins$hasExtendedVelocity() {
        return this.raven_dnd_origins$hasExtendedVelocity;
    }

    @Override
    public double raven_dnd_origins$getExtendedX() {
        return this.raven_dnd_origins$extendedX;
    }

    @Override
    public double raven_dnd_origins$getExtendedY() {
        return this.raven_dnd_origins$extendedY;
    }

    @Override
    public double raven_dnd_origins$getExtendedZ() {
        return this.raven_dnd_origins$extendedZ;
    }
}
