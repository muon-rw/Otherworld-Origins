package dev.muon.otherworldorigins.client.compat;

import com.github.exopandora.shouldersurfing.client.ShoulderSurfingImpl;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraftforge.fml.ModList;

/**
 * Client-side integration with Shoulder Surfing Reloaded.
 * Makes the player look at the crosshair target when casting spells.
 */
public class ShoulderSurfingIntegration {
    private static final String SHOULDER_SURFING_MOD_ID = "shouldersurfing";
    private static Boolean isShoulderSurfingLoaded = null;

    private static boolean isShoulderSurfingLoaded() {
        if (isShoulderSurfingLoaded == null) {
            isShoulderSurfingLoaded = ModList.get().isLoaded(SHOULDER_SURFING_MOD_ID);
        }
        return isShoulderSurfingLoaded;
    }

    private static ShoulderSurfingImpl getShoulderSurfing() {
        if (!isShoulderSurfingLoaded()) {
            return null;
        }
        return ShoulderSurfingImpl.getInstance();
    }

    private static boolean isShoulderSurfing() {
        ShoulderSurfingImpl instance = getShoulderSurfing();
        return instance != null && instance.isShoulderSurfing();
    }

    public static void lookAtCrosshairTarget() {
        ShoulderSurfingImpl instance = getShoulderSurfing();
        if (instance != null) {
            instance.lookAtCrosshairTarget();
        }
    }

    public static void lookAtCrosshairTargetIfShoulderSurfing() {
        if (isShoulderSurfing()) {
            lookAtCrosshairTarget();
        }
    }

    public static boolean shouldAimAtTarget() {
        return isCastingContinuousSpell();
    }

    private static boolean isContinuousSpell(CastType castType) {
        return castType == CastType.CONTINUOUS;
    }

    private static boolean isCastingContinuousSpell() {
        return ClientMagicData.isCasting() && isContinuousSpell(ClientMagicData.getCastType());
    }
}
