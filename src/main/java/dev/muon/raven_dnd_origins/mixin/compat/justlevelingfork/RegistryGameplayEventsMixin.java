package dev.muon.raven_dnd_origins.mixin.compat.justlevelingfork;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.seniors.justlevelingfork.registry.RegistryEffects;
import com.seniors.justlevelingfork.registry.RegistryGameplayEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = RegistryGameplayEvents.class, remap = false)
public class RegistryGameplayEventsMixin {

    /**
     * Disables the Resistance effect for Diamond Skin; we apply flat damage reduction in the damage
     * pipeline instead (15% base, 30% while sneaking).
     */
    @WrapWithCondition(
            method = "updatePlayerPassives",
            at = @At(value = "INVOKE", target = "Lcom/seniors/justlevelingfork/registry/RegistryEffects$addEffect;add(II)V")
    )
    private static boolean skipDiamondSkinResistance(RegistryEffects.addEffect instance, int duration, int amplifier) {
        return false;
    }
}
