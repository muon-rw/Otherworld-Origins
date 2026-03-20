package dev.muon.otherworldorigins.mixin.compat.irons_spellbooks;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.player.ClientInputEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ClientInputEvents.class, remap = false)
public class ClientInputEventsMixin {

    @ModifyExpressionValue(
            method = "onUseInput",
            at = @At(value = "INVOKE", target = "Lio/redspace/ironsspellbooks/player/ClientMagicData;isCasting()Z")
    )
    private static boolean otherworldorigins$allowAttackDuringCommandCast(boolean isCasting) {
        if (isCasting && ClientMagicDataAccessor.otherworldorigins$getPlayerMagicData().getCastSource() == CastSource.COMMAND) {
            return false;
        }
        return isCasting;
    }
}
