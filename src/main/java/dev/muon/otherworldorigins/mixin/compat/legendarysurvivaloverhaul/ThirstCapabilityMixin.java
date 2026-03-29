package dev.muon.otherworldorigins.mixin.compat.legendarysurvivaloverhaul;

import dev.muon.otherworldorigins.power.UndeadVitalsPower;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sfiomn.legendarysurvivaloverhaul.common.capabilities.thirst.ThirstCapability;

@Mixin(value = ThirstCapability.class, remap = false)
public class ThirstCapabilityMixin {

    @Inject(method = "tickUpdate", at = @At("HEAD"), cancellable = true)
    private void otherworldorigins$undeadSkipThirstDrain(Player player, Level level, TickEvent.Phase phase, CallbackInfo ci) {
        if (phase != TickEvent.Phase.END || !UndeadVitalsPower.has(player)) {
            return;
        }
        ThirstCapability self = (ThirstCapability) (Object) this;
        self.setHydrationLevel(ThirstCapability.MAX_HYDRATION);
        self.setThirstExhaustion(0f);
        ci.cancel();
    }
}
