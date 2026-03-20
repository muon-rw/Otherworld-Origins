package dev.muon.otherworldorigins.mixin.compat.irons_spellbooks;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.player.ServerPlayerEvents;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ServerPlayerEvents.class, remap = false)
public class ServerPlayerEventsMixin {

    @ModifyExpressionValue(
            method = "onUseItem",
            at = @At(value = "INVOKE", target = "Lio/redspace/ironsspellbooks/api/magic/MagicData;isCasting()Z")
    )
    private static boolean otherworldorigins$allowItemUseDuringCommandCast(boolean isCasting,
                                                                          PlayerInteractEvent.RightClickItem event) {
        if (isCasting) {
            MagicData magicData = MagicData.getPlayerMagicData(event.getEntity());
            if (magicData.getCastSource() == CastSource.COMMAND) {
                return false;
            }
        }
        return isCasting;
    }
}
