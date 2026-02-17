package dev.muon.otherworldorigins.mixin.origins_patches;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.edwinmindcraft.apoli.common.component.PowerContainer;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = PowerContainer.class, remap = false)
public abstract class PowerContainerMixin {

    @WrapOperation(
            method = "addPower",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/apache/logging/log4j/Logger;info(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"
            )
    )
    private void logSubpowersAsDebug(Logger instance, String s, Object o, Object p, Operation<Void> original) {
        instance.debug(s, o, p);
        // original.call(instance, s, o, p)`
    }
}
