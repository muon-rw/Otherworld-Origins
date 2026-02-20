package dev.muon.otherworldorigins.mixin.compat.irons_spellbooks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.ICastData;
import io.redspace.ironsspellbooks.spells.ender.TeleportSpell;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = TeleportSpell.class, remap = false)
public class TeleportSpellMixin {
    @WrapOperation(method = "onCast", at = @At(value = "INVOKE", target = "Lio/redspace/ironsspellbooks/api/magic/MagicData;getAdditionalCastData()Lio/redspace/ironsspellbooks/api/spells/ICastData;"), remap = false)
    private ICastData wrapGetCastData(MagicData instance, Operation<ICastData> original) {
        ICastData data = original.call(instance);
        return data instanceof TeleportSpell.TeleportData ? data : null;
    }
}
