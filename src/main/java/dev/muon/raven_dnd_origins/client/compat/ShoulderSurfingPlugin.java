package dev.muon.raven_dnd_origins.client.compat;

import com.github.exopandora.shouldersurfing.api.client.event.handler.ComputeTargetCameraOffsetEventHandler;
import com.github.exopandora.shouldersurfing.api.event.IEventBus;
import com.github.exopandora.shouldersurfing.api.plugin.IShoulderSurfingPlugin;

/**
 * Plugin for Shoulder Surfing Reloaded. The offset handler pulls the camera back for larger
 * shapeshift forms.
 */
public class ShoulderSurfingPlugin implements IShoulderSurfingPlugin {

    @Override
    public void register(IEventBus eventBus) {
        ComputeTargetCameraOffsetEventHandler cameraOffset =
                event -> event.setResult(ShoulderSurfingIntegration.scaleCameraOffsetForShapeshift(event.getResult()));
        eventBus.register(cameraOffset);
    }
}
