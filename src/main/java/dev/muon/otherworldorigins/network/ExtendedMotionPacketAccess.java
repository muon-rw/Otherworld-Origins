package dev.muon.otherworldorigins.network;

/**
 * Duck-type accessor implemented via mixin on {@link net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket}.
 * Lets the client-side handler read the unclamped delta we piggyback on the wire
 * when an entity's velocity exceeds the ±3.9/component limit that the packet's short encoding enforces.
 */
public interface ExtendedMotionPacketAccess {

    boolean otherworldorigins$hasExtendedVelocity();

    double otherworldorigins$getExtendedX();

    double otherworldorigins$getExtendedY();

    double otherworldorigins$getExtendedZ();
}
