package dev.muon.otherworldorigins.capability;

import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Tracks the most recent player to insert a brewing ingredient into a brewing stand.
 * Persisted via {@link BrewerTrackerCapability}'s {@code ICapabilitySerializable}, so a brew
 * paused by chunk unload or server restart still credits the right brewer when it completes.
 */
public interface IBrewerTracker {
    Optional<UUID> getBrewer();

    void setBrewer(@Nullable UUID brewerUuid);
}
