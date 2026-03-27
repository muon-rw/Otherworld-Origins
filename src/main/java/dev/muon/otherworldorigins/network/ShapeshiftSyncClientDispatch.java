package dev.muon.otherworldorigins.network;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public final class ShapeshiftSyncClientDispatch {

    @Nullable
    private static volatile Consumer<ShapeshiftSyncMessage> handler;

    private ShapeshiftSyncClientDispatch() {}

    public static void registerHandler(Consumer<ShapeshiftSyncMessage> clientHandler) {
        handler = clientHandler;
    }

    @Nullable
    public static Consumer<ShapeshiftSyncMessage> getHandler() {
        return handler;
    }
}
