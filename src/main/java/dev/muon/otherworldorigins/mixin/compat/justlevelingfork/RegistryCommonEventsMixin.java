package dev.muon.otherworldorigins.mixin.compat.justlevelingfork;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.seniors.justlevelingfork.registry.RegistryEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = com.seniors.justlevelingfork.registry.RegistryCommonEvents.class, remap = false)
public class RegistryCommonEventsMixin {

    /**
     * Disable the Resistance effect for Diamond Skin - we use flat damage reduction
     * in the damage pipeline instead (15% base, 30% while sneaking).
     *
     */
    @WrapOperation(
            method = "onPlayerTick",
            at = @At(
                    value = "NEW",
                    target = "com/seniors/justlevelingfork/registry/RegistryEffects$addEffect",
                    ordinal = 1,
                    remap = false
            )
    )
    private static RegistryEffects.addEffect otherworld$disableDiamondSkinResistanceEffect(ServerPlayer serverPlayer, boolean condition, MobEffect effect, Operation<RegistryEffects.addEffect> operation) {
        return operation.call(serverPlayer, false, effect);
    }
}
