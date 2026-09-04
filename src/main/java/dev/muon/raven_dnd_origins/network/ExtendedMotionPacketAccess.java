package dev.muon.raven_dnd_origins.network;

/**
 * Duck-type accessor implemented via mixin on {@link net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket}.
 * Lets the client-side handler read the unclamped delta we piggyback on the wire
 * when an entity's velocity exceeds the ±3.9/component limit that the packet's short encoding enforces.
 */
public interface ExtendedMotionPacketAccess {

    boolean raven_dnd_origins$hasExtendedVelocity();

    double raven_dnd_origins$getExtendedX();

    double raven_dnd_origins$getExtendedY();

    double raven_dnd_origins$getExtendedZ();
}
