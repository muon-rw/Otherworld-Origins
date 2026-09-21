package dev.muon.raven_dnd_origins.mixin.compat.immersive_melodies;

import dev.muon.raven_dnd_origins.util.LivePerformanceTracker;
import immersive_melodies.network.c2s.NoteBroadcastRequest;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NoteBroadcastRequest.class, remap = false)
public class NoteBroadcastRequestMixin {

    @Shadow
    @Final
    private int tone;

    @Shadow
    @Final
    private int velocity;

    @Inject(method = "handle", at = @At("HEAD"))
    private void raven_dnd_origins$trackLivePerformance(Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer) {
            LivePerformanceTracker.onNote(player, tone, velocity);
        }
    }
}
