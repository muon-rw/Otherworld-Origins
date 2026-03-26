package dev.muon.otherworldorigins.network;

/**
 * Raised limits for custom payloads and Calio dynamic registry splitting.
 * <p>
 * Vanilla caps (1.20.1 Forge): clientbound play 1 MiB, clientbound login 1 MiB,
 * serverbound play 32 KiB, serverbound login 1 MiB. Calio splits registry sync at 1 MiB,
 * which multiplies packets and can worsen ordering races on large datapacks.
 * <p>
 * Mixins use {@link Math#max} with these constants so MixinExtras-chained injectors from
 * other mods can raise the ceiling further. Mods that use non-chaining {@code @Redirect}
 * or {@code @Overwrite} on the same sites can still conflict.
 * <p>
 * These values deliberately stay below {@link Integer#MAX_VALUE} to avoid accidental
 * overflow in comparison code and to keep allocations bounded.
 */
public final class OtherworldOriginsNetworkLimits {
    private OtherworldOriginsNetworkLimits() {}

    /** Must be large enough for the biggest encoded Calio {@code S2CDynamicRegistryPacket} chunk. */
    public static final int CUSTOM_PAYLOAD_MAX_BYTES = 256 * 1024 * 1024;

    /**
     * Threshold passed to Calio's {@code splitPackets}; keep aligned with
     * {@link #CUSTOM_PAYLOAD_MAX_BYTES} so one registry usually fits one packet.
     */
    public static final int CALIO_DYNAMIC_REGISTRY_CHUNK_SOFT_LIMIT = CUSTOM_PAYLOAD_MAX_BYTES;
}
