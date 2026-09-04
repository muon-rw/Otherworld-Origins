package dev.muon.raven_dnd_origins.mixin.compat.hardcorerevival;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.overgrown.apoli.condition.context.DamageCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.PowerTypeRegistry;
import dev.overgrown.apoli.power.builtin.PreventDeathPower;
import net.blay09.mods.hardcorerevival.handler.KnockoutHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hardcore Revival knocks the player out from a Balm death-event listener at High priority and cancels the
 * event, so Apoli's own listener (normal priority) never runs and {@code apoli:prevent_death} is skipped:
 * wildshape's heal and origin-reset actions never execute. Declaring knockout disabled for a death that
 * prevent_death is going to catch leaves the death event uncancelled, so Apoli gets its normal turn.
 * {@link dev.muon.raven_dnd_origins.power.ArcaneWardPreventDeathPower} needs no such yield: it listens at
 * HIGHEST, ahead of the knockout, and knockout is still the right outcome when its mana check fails.
 */
@Mixin(value = KnockoutHandler.class, remap = false)
public class KnockoutHandlerPreventDeathMixin {

    @ModifyReturnValue(method = "isKnockoutEnabledFor", at = @At("RETURN"))
    private static boolean raven_dnd_origins$yieldToPreventDeath(boolean original, ServerPlayer player, DamageSource damageSource) {
        return original && !raven_dnd_origins$hasArmedPreventDeath(player, damageSource);
    }

    /**
     * Mirrors {@code PreventDeathHandler.tryPrevent}'s selection without its side effects, and with the
     * same zero damage amount Apoli passes on the death event.
     */
    @Unique
    private static boolean raven_dnd_origins$hasArmedPreventDeath(ServerPlayer player, DamageSource damageSource) {
        PowerContainer container = PowerContainer.of(player);
        if (container == null || container.isEmpty()) {
            return false;
        }
        DamageCtx damageCtx = null;
        for (ResourceLocation powerId : container.allPowers()) {
            Power power = ApoliPowers.get(powerId);
            if (power == null || container.isSuppressed(powerId)) continue;
            if (!(PowerTypeRegistry.get(power.typeId()) instanceof PreventDeathPower)) continue;
            if (!(power.config() instanceof PreventDeathPower.Config cfg)) continue;
            if (power.condition().isPresent()
                    && !power.condition().get().test(new EntityCtx(player, player.level()))) continue;
            if (cfg.damageCondition().isPresent()) {
                if (damageCtx == null) damageCtx = new DamageCtx(damageSource, player, player.level(), 0.0F);
                if (!cfg.damageCondition().get().test(damageCtx)) continue;
            }
            return true;
        }
        return false;
    }
}
