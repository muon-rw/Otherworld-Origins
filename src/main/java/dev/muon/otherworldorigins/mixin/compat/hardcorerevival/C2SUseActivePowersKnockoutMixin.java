package dev.muon.otherworldorigins.mixin.compat.hardcorerevival;

import io.github.edwinmindcraft.apoli.common.network.C2SUseActivePowers;
import net.blay09.mods.hardcorerevival.HardcoreRevival;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(value = C2SUseActivePowers.class, remap = false)
public class C2SUseActivePowersKnockoutMixin {

    @Inject(method = "handle", at = @At("HEAD"), cancellable = true)
    private void otherworldorigins$blockActivePowerWhileKnockedOut(
            Supplier<NetworkEvent.Context> contextSupplier,
            CallbackInfo ci
    ) {
        ServerPlayer sender = contextSupplier.get().getSender();
        if (sender != null && HardcoreRevival.getRevivalData(sender).isKnockedOut()) {
            contextSupplier.get().setPacketHandled(true);
            ci.cancel();
        }
    }
}
