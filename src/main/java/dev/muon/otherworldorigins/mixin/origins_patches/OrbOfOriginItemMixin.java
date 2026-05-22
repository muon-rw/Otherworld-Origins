package dev.muon.otherworldorigins.mixin.origins_patches;

import dev.muon.otherworldorigins.selection.SelectionSessions;
import dev.muon.otherworldorigins.selection.SessionKind;
import io.github.apace100.origins.content.OrbOfOriginItem;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import net.minecraft.core.Holder;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Routes the (untargeted) vanilla Orb of Origin through the {@link SelectionSessions} system.
 * Origins' own {@code use} clears every layer and fires {@code S2COpenOriginScreen} with no
 * persisted state, so a player who relogs mid-selection recovers only via reconcile guesswork and
 * is never granted selection invulnerability on the first pass. Re-issuing the full wipe as a
 * proper {@code INITIAL_CREATION} session makes the orb behave exactly like first-time character
 * creation: persisted, relog-safe, invulnerable, with the final confirmation screen on completion.
 *
 * <p>A <em>targeted</em> orb (carrying an NBT {@code Targets} list: an admin/datapack grant of
 * specific origins, not a wipe) is left to Origins untouched.
 */
@Mixin(OrbOfOriginItem.class)
public abstract class OrbOfOriginItemMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void otherworld$routeFullWipeThroughSession(Level level, Player player, InteractionHand hand,
                                                        CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (otherworld$isTargetedOrb(stack)) {
            return;
        }
        List<ResourceLocation> layers = new ArrayList<>();
        for (Holder.Reference<OriginLayer> layer : OriginsAPI.getActiveLayers()) {
            layer.unwrapKey().ifPresent(key -> layers.add(key.location()));
        }
        boolean started = SelectionSessions.beginCleared(serverPlayer, layers, SessionKind.INITIAL_CREATION);
        if (started && !player.isCreative()) {
            stack.shrink(1);
        }
        cir.setReturnValue(InteractionResultHolder.consume(stack));
    }

    /** A targeted orb carries a non-empty {@code Targets} list — a deliberate grant, not a wipe. */
    @Unique
    private static boolean otherworld$isTargetedOrb(ItemStack stack) {
        return stack.hasTag()
                && stack.getTag().contains("Targets", Tag.TAG_LIST)
                && !stack.getTag().getList("Targets", Tag.TAG_COMPOUND).isEmpty();
    }
}
