package dev.muon.raven_dnd_origins.client.compat;

import com.github.exopandora.shouldersurfing.api.client.event.handler.ComputePlayerAimStateEventHandler;
import com.github.exopandora.shouldersurfing.api.client.event.handler.ComputeTargetCameraOffsetEventHandler;
import com.github.exopandora.shouldersurfing.api.event.IEventBus;
import com.github.exopandora.shouldersurfing.api.plugin.IShoulderSurfingPlugin;

/**
 * Plugin for Shoulder Surfing Reloaded. The aim-state handler makes the player continuously aim at
 * the crosshair target while casting a continuous spell; the offset handler pulls the camera back
 * for larger shapeshift forms.
 */
public class ShoulderSurfingPlugin implements IShoulderSurfingPlugin {

    @Override
    public void register(IEventBus eventBus) {
        ComputePlayerAimStateEventHandler aimState = event -> {
            if (!event.getResult() && ShoulderSurfingIntegration.shouldAimAtTarget()) {
                event.setResult(true);
            }
        };
        ComputeTargetCameraOffsetEventHandler cameraOffset =
                event -> event.setResult(ShoulderSurfingIntegration.scaleCameraOffsetForShapeshift(event.getResult()));
        eventBus.register(aimState);
        eventBus.register(cameraOffset);
    }
}
