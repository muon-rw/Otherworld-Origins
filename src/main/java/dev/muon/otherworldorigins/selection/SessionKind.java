package dev.muon.otherworldorigins.selection;

/**
 * Why a selection screen was opened. Persisted with the {@link SelectionSession} so a player who
 * disconnects mid-prompt resumes in the same mode instead of being dropped into full character
 * creation.
 *
 * <p>Declaration order is merge precedence: when a new prompt fires while a session is already
 * open, the merged session keeps the earlier-declared kind (see {@link #dominant}).
 */
public enum SessionKind {
    /** First-time character creation: all layers, full confirmation screen on completion. */
    INITIAL_CREATION,
    /** Ancestry/Vocation orb or single-layer re-pick: scoped confirmation screen on completion. */
    RESELECTION,
    /** Level-gated feat/discipline layers unlocked on level-up: no confirmation screen. */
    LEVEL_UP,
    /** Power-driven prompt such as wildshape: no confirmation screen. */
    POWER_PROMPT;

    /** The higher-precedence (earlier-declared) of two kinds, used when merging sessions. */
    public static SessionKind dominant(SessionKind a, SessionKind b) {
        return a.ordinal() <= b.ordinal() ? a : b;
    }
}
