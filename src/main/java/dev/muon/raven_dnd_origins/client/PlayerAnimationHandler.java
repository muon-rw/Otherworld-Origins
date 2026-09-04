package dev.muon.raven_dnd_origins.client;

import dev.kosmx.playerAnim.api.IPlayable;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import dev.muon.raven_dnd_origins.RavenDndOrigins;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Client-side handler for playing player animations via the player-animator API.
 * Animations must be registered in our animation layer (see ModEventsClient).
 */
public class PlayerAnimationHandler {

    public static final ResourceLocation ANIMATION_LAYER_ID = RavenDndOrigins.loc("animation");

    public static void playAnimation(UUID playerId, ResourceLocation animationId) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;

        var player = level.getPlayerByUUID(playerId);
        if (!(player instanceof AbstractClientPlayer clientPlayer)) return;

        IPlayable playable = PlayerAnimationRegistry.getAnimation(animationId);
        if (playable == null) {
            RavenDndOrigins.LOGGER.debug("Unknown player animation: {}", animationId);
            return;
        }

        var animationData = PlayerAnimationAccess.getPlayerAssociatedData(clientPlayer).get(ANIMATION_LAYER_ID);
        if (!(animationData instanceof ModifierLayer<?> layer)) return;

        @SuppressWarnings("unchecked")
        var modifierLayer = (ModifierLayer<IAnimation>) layer;
        modifierLayer.replaceAnimationWithFade(
                AbstractFadeModifier.standardFadeIn(2, Ease.INOUTSINE), playable.playAnimation(), true);
    }
}
