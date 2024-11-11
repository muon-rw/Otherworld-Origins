package dev.muon.otherworldorigins.mixin.compat.irons_spellbooks;

import dev.muon.otherworldorigins.util.CastingRestrictions;
import io.redspace.ironsspellbooks.item.CastingItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(CastingItem.class)
public class CastingItemMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void otherworldorigins$restrictCasting(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (!CastingRestrictions.isCastingAllowed(player)) {
            player.displayClientMessage(
                    Component.literal("You are not attuned to magic!")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            cir.setReturnValue(InteractionResultHolder.fail(player.getItemInHand(hand)));
        }
    }

}