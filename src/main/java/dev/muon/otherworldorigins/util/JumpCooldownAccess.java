package dev.muon.otherworldorigins.util;

/**
 * Player-only storage for {@link dev.muon.otherworldorigins.power.JumpCooldownPower} (client ticks, local player only).
 */
public interface JumpCooldownAccess {
    int otherworldorigins$getJumpCooldownRemaining();

    void otherworldorigins$setJumpCooldownRemaining(int ticks);
}
