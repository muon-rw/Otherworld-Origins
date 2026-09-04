package dev.muon.raven_dnd_origins.selection;

import javax.annotation.Nullable;

/**
 * Client-side copy of the player's pending {@link SelectionSession}, pushed by the server. Read by
 * the render-tick handler to open the selection screen with the right layers and mode; purely
 * transient; the server holds the authoritative copy.
 */
public final class ClientSelectionState {

    @Nullable
    private static SelectionSession current;

    private ClientSelectionState() {}

    public static void set(@Nullable SelectionSession session) {
        current = session;
    }

    @Nullable
    public static SelectionSession get() {
        return current;
    }

    public static void clear() {
        current = null;
    }
}
