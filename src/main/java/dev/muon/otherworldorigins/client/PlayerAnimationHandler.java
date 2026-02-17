package dev.muon.otherworldorigins.client;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import dev.muon.otherworldorigins.OtherworldOrigins;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-side handler for playing player animations via the player-animator API.
 * Animations must be registered in our animation layer (see ModEventsClient).
 */
public class PlayerAnimationHandler {

    public static final ResourceLocation ANIMATION_LAYER_ID = OtherworldOrigins.loc("animation");

    public static void playAnimation(java.util.UUID playerId, ResourceLocation animationId) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;

        var player = level.getPlayerByUUID(playerId);
        if (!(player instanceof AbstractClientPlayer clientPlayer)) return;

        var keyframeAnimation = PlayerAnimationRegistry.getAnimation(animationId);
        if (keyframeAnimation == null) {
            OtherworldOrigins.LOGGER.debug("Unknown player animation: {}", animationId);
            return;
        }

        var animationData = PlayerAnimationAccess.getPlayerAssociatedData(clientPlayer).get(ANIMATION_LAYER_ID);
        if (!(animationData instanceof ModifierLayer<?> layer)) return;

        @SuppressWarnings("unchecked")
        var modifierLayer = (ModifierLayer<IAnimation>) layer;
        var animation = new KeyframeAnimationPlayer(keyframeAnimation);
        modifierLayer.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(2, Ease.INOUTSINE), animation, true);
    }
}
