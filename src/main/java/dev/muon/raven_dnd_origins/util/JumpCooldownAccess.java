package dev.muon.raven_dnd_origins.util;

/**
 * Player-only storage for {@link dev.muon.raven_dnd_origins.power.JumpCooldownPower} (client ticks, local player only).
 */
public interface JumpCooldownAccess {
    int raven_dnd_origins$getJumpCooldownRemaining();

    void raven_dnd_origins$setJumpCooldownRemaining(int ticks);
}
