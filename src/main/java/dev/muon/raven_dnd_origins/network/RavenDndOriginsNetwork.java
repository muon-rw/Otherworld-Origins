package dev.muon.raven_dnd_origins.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class RavenDndOriginsNetwork {

    private static final String PROTOCOL_VERSION = "1";

    private RavenDndOriginsNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        registrar.playToServer(BeginReselectionMessage.TYPE, BeginReselectionMessage.STREAM_CODEC,
                BeginReselectionMessage::handle);
        registrar.playToServer(C2SRevertLayerOriginsMessage.TYPE, C2SRevertLayerOriginsMessage.STREAM_CODEC,
                C2SRevertLayerOriginsMessage::handle);
        registrar.playToServer(GiveStarterKitMessage.TYPE, GiveStarterKitMessage.STREAM_CODEC,
                GiveStarterKitMessage::handle);
        registrar.playToServer(RequestContainerSyncMessage.TYPE, RequestContainerSyncMessage.STREAM_CODEC,
                RequestContainerSyncMessage::handle);
        registrar.playToServer(RequestFullSyncMessage.TYPE, RequestFullSyncMessage.STREAM_CODEC,
                RequestFullSyncMessage::handle);
        registrar.playToServer(RequestServerStateDumpMessage.TYPE, RequestServerStateDumpMessage.STREAM_CODEC,
                RequestServerStateDumpMessage::handle);
        registrar.playToServer(ResetLeveledLayersMessage.TYPE, ResetLeveledLayersMessage.STREAM_CODEC,
                ResetLeveledLayersMessage::handle);
        registrar.playToServer(ResetOriginsMessage.TYPE, ResetOriginsMessage.STREAM_CODEC,
                ResetOriginsMessage::handle);
        registrar.playToServer(RespecAptitudesMessage.TYPE, RespecAptitudesMessage.STREAM_CODEC,
                RespecAptitudesMessage::handle);
        registrar.playToServer(SelectionSessionFinishedMessage.TYPE, SelectionSessionFinishedMessage.STREAM_CODEC,
                SelectionSessionFinishedMessage::handle);
        registrar.playToServer(WildshapeCantripHeldMessage.TYPE, WildshapeCantripHeldMessage.STREAM_CODEC,
                WildshapeCantripHeldMessage::handle);

        registrar.playToClient(CloseCurrentScreenMessage.TYPE, CloseCurrentScreenMessage.STREAM_CODEC,
                CloseCurrentScreenMessage::handle);
        registrar.playToClient(DumpClientStateMessage.TYPE, DumpClientStateMessage.STREAM_CODEC,
                DumpClientStateMessage::handle);
        registrar.playToClient(OpenFinalConfirmScreenMessage.TYPE, OpenFinalConfirmScreenMessage.STREAM_CODEC,
                OpenFinalConfirmScreenMessage::handle);
        registrar.playToClient(PlayPlayerAnimationPacket.TYPE, PlayPlayerAnimationPacket.STREAM_CODEC,
                PlayPlayerAnimationPacket::handle);
        registrar.playToClient(ShapeshiftSyncMessage.TYPE, ShapeshiftSyncMessage.STREAM_CODEC,
                ShapeshiftSyncMessage::handle);
        registrar.playToClient(SyncSelectionSessionMessage.TYPE, SyncSelectionSessionMessage.STREAM_CODEC,
                SyncSelectionSessionMessage::handle);
    }
}
