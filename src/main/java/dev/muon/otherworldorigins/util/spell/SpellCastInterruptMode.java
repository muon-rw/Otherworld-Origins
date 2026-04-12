package dev.muon.otherworldorigins.util.spell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * How cast actions handle the actor already casting a spell when a new cast is requested.
 */
public enum SpellCastInterruptMode {
    /**
     * Players: Iron's {@code Utils.serverSideCancelCast}. Non-players: {@link SpellCastUtil#forceCompleteCurrentCastIfAny}
     * (no dedicated cancel path).
     */
    CANCEL,
    /**
     * Run {@link SpellCastUtil#forceCompleteCurrentCastIfAny} so the in-progress spell resolves immediately.
     */
    FORCE_COMPLETE,
    /**
     * Do not change casting state; the action stops and the current cast continues uninterrupted.
     */
    FAIL;

    public static final Codec<SpellCastInterruptMode> CODEC = Codec.STRING.comapFlatMap(
            SpellCastInterruptMode::fromJson,
            SpellCastInterruptMode::serializedName
    );

    private static DataResult<SpellCastInterruptMode> fromJson(String raw) {
        return switch (raw) {
            case "cancel" -> DataResult.success(CANCEL);
            case "force_complete" -> DataResult.success(FORCE_COMPLETE);
            case "fail" -> DataResult.success(FAIL);
            default -> DataResult.error(() -> "Unknown interrupt_mode \"" + raw + "\"; expected cancel, force_complete, or fail");
        };
    }

    public String serializedName() {
        return switch (this) {
            case CANCEL -> "cancel";
            case FORCE_COMPLETE -> "force_complete";
            case FAIL -> "fail";
        };
    }
}
