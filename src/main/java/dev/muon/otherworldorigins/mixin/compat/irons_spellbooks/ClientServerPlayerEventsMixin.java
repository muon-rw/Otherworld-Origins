package dev.muon.otherworldorigins.mixin.compat.irons_spellbooks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.player.ServerPlayerEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Consumer;

/**
 * Client-side companion to ServerPlayerEventsMixin.
 * Handles the client-side right-click cancellation in onUseItem which lives inside a lambda
 * passed to MinecraftInstanceHelper.ifPlayerPresent.
 */
@Mixin(value = ServerPlayerEvents.class, remap = false)
public class ClientServerPlayerEventsMixin {

    @WrapOperation(
            method = "onUseItem",
            at = @At(value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/util/MinecraftInstanceHelper;ifPlayerPresent(Ljava/util/function/Consumer;)V")
    )
    private static void otherworldorigins$skipClientCancelForCommandCast(Consumer<?> consumer,
                                                                        Operation<Void> operation) {
        if (ClientMagicData.isCasting()
                && ClientMagicDataAccessor.otherworldorigins$getPlayerMagicData().getCastSource() == CastSource.COMMAND) {
            return;
        }
        operation.call(consumer);
    }
}
