package dev.muon.raven_dnd_origins.mixin.compat.legendarysurvivaloverhaul;

import dev.muon.raven_dnd_origins.power.ThirstImmunityPower;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sfiomn.legendarysurvivaloverhaul.common.attachments.thirst.ThirstAttachment;

@Mixin(value = ThirstAttachment.class, remap = false)
public class ThirstAttachmentMixin {

    @Inject(method = "tickUpdate", at = @At("HEAD"), cancellable = true)
    private void raven_dnd_origins$undeadSkipThirstDrain(Player player, Level level, boolean isStart, CallbackInfo ci) {
        if (isStart || !ThirstImmunityPower.has(player)) {
            return;
        }
        ThirstAttachment self = (ThirstAttachment) (Object) this;
        self.setHydrationLevel(ThirstAttachment.MAX_HYDRATION);
        self.setExhaustion(0f);
        ci.cancel();
    }
}
