package dev.muon.otherworldorigins.mixin.compat.hardcorerevival;

import dev.muon.otherworldorigins.power.ArcaneWardPreventDeathPower;
import io.github.edwinmindcraft.apoli.common.power.PreventDeathPower;
import net.blay09.mods.balm.api.event.LivingDamageEvent;
import net.blay09.mods.hardcorerevival.HardcoreRevival;
import net.blay09.mods.hardcorerevival.handler.KnockoutHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hardcore Revival caps lethal damage at {@code health - 1} on {@link LivingDamageEvent} and triggers knockout,
 * so {@link net.minecraftforge.event.entity.living.LivingDeathEvent} never fires. That means Apoli's
 * {@code origins:prevent_death} (runs on LivingDeathEvent at HIGHEST) is skipped — wildshape's heal and
 * origin-reset actions never execute. Run prevent_death ourselves before the knockout branch.
 */
@Mixin(value = KnockoutHandler.class, remap = false)
public class KnockoutHandlerPreventDeathMixin {

    @Inject(method = "onPlayerDamage", at = @At("HEAD"), cancellable = true)
    private static void otherworldorigins$runPreventDeathBeforeKnockout(LivingDamageEvent event, CallbackInfo ci) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;
        if (HardcoreRevival.getRevivalData(player).isKnockedOut()) return;

        float damage = event.getDamageAmount();
        if (player.getHealth() - damage > 0.0F) return;

        DamageSource source = event.getDamageSource();

        if (PreventDeathPower.tryPreventDeath(player, source, damage)) {
            event.setDamageAmount(0);
            ci.cancel();
            return;
        }

        if (ArcaneWardPreventDeathPower.tryPreventDeath(player, source, damage)) {
            event.setDamageAmount(0);
            ci.cancel();
        }
    }
}
