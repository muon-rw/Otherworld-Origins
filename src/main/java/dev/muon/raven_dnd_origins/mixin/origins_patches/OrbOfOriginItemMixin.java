package dev.muon.raven_dnd_origins.mixin.origins_patches;

import dev.muon.raven_dnd_origins.selection.SelectionSessions;
import dev.overgrown.origins.item.OrbOfOriginItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


/**
 * Routes the Orb of Origin through the {@link SelectionSessions} system. Origins' own {@code use}
 * clears every layer and opens the choose screen with no persisted state, so a player who relogs
 * mid-selection recovers only via reconcile guesswork and is never granted selection invulnerability
 * on the first pass. Re-issuing the full wipe as an {@code INITIAL_CREATION} session makes the orb
 * behave exactly like first-time character creation: persisted, relog-safe, invulnerable, with the
 * final confirmation screen on completion.
 */
@Mixin(value = OrbOfOriginItem.class, remap = false)
public abstract class OrbOfOriginItemMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void raven_dnd_origins$routeFullWipeThroughSession(Level world, Player user, InteractionHand hand,
                                                        CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (world.isClientSide() || !(user instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ItemStack stack = user.getItemInHand(hand);
        boolean started = SelectionSessions.beginFullCreation(serverPlayer);
        if (started && !user.isCreative()) {
            stack.shrink(1);
        }
        cir.setReturnValue(InteractionResultHolder.consume(stack));
    }
}
