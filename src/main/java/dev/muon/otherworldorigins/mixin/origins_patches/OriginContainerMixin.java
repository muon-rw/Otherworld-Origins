package dev.muon.otherworldorigins.mixin.origins_patches;

import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.origins.common.OriginsCommon;
import io.github.edwinmindcraft.origins.common.capabilities.OriginContainer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Origins queues origin sync on the player tick with a 20-tick cooldown. Nested {@code setOrigin}
 * calls (e.g. choosing an upgraded elemental discipline then {@code action_on_callback} clearing a
 * lower tier in the same stack) often invoke {@link OriginContainer#synchronize()} when the flag is
 * already true, so the outer call is a no-op and the client can observe a stale layer map until the
 * next debounced send. Pushing {@link OriginContainer#getSynchronizationPacket()} immediately on
 * every {@code synchronize()} keeps the client aligned with the server's map after each mutation.
 *
 * <p>The flush is gated on the target {@link ServerPlayer} already being registered in the
 * server's {@link net.minecraft.server.players.PlayerList}. During
 * {@link net.minecraftforge.event.entity.player.PlayerEvent.Clone} on respawn, Origins calls
 * {@code deserializeNBT} → {@code setOriginInternal(..., false)} → {@code synchronize} for every
 * layer while the new {@code ServerPlayer} is mid-{@code PlayerList#respawn}: its connection has
 * already been moved over from the old player, but {@code setId} hasn't run yet, the entity isn't
 * in the level, and the player isn't in the player list. Sending {@code TRACKING_ENTITY_AND_SELF}
 * packets keyed on the new player's (still-fresh) entity id lands on a client that has no matching
 * entity, gets dropped, and leaves the client's origin container empty ("You can not have any
 * origins."). The standard debounced tick send covers that case once the respawn completes.
 */
@Mixin(value = OriginContainer.class, remap = false)
public abstract class OriginContainerMixin {

    @Inject(method = "synchronize", at = @At("TAIL"), remap = false)
    private void otherworldorigins$flushOriginSyncNow(CallbackInfo ci) {
        Player owner = ((OriginContainer) (Object) this).getOwner();
        if (owner.level().isClientSide() || !(owner instanceof ServerPlayer serverPlayer)) {
            return;
        }
        MinecraftServer server = serverPlayer.getServer();
        if (server == null || server.getPlayerList().getPlayer(serverPlayer.getUUID()) != serverPlayer) {
            return;
        }
        OriginContainer self = (OriginContainer) (Object) this;
        OriginsCommon.CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> serverPlayer),
                self.getSynchronizationPacket());
        ApoliAPI.synchronizePowerContainer(owner);
        self.validateSynchronization();
    }
}
