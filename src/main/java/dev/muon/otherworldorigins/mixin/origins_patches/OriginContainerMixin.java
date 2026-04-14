package dev.muon.otherworldorigins.mixin.origins_patches;

import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.origins.common.OriginsCommon;
import io.github.edwinmindcraft.origins.common.capabilities.OriginContainer;
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
 */
@Mixin(value = OriginContainer.class, remap = false)
public abstract class OriginContainerMixin {

    @Inject(method = "synchronize", at = @At("TAIL"), remap = false)
    private void otherworldorigins$flushOriginSyncNow(CallbackInfo ci) {
        Player owner = ((OriginContainer) (Object) this).getOwner();
        if (owner.level().isClientSide() || !(owner instanceof ServerPlayer serverPlayer)) {
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
