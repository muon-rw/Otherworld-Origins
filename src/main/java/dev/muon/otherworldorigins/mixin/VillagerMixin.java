package dev.muon.otherworldorigins.mixin;

import dev.muon.otherworldorigins.power.TradeDiscountPower;
import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.apoli.util.modifier.ModifierUtil;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public class VillagerMixin {

    @Inject(method = "updateSpecialPrices", at = @At("TAIL"))
    private void onUpdateSpecialPrices(Player player, CallbackInfo ci) {
        double totalDiscount = PowerHolderComponent.getPowers(player, TradeDiscountPower.class).stream()
                .filter(TradeDiscountPower::isActive)
                .mapToDouble(powerType -> ModifierUtil.applyModifiers(player, powerType.getModifiers(), 0.0))
                .sum();

        if (totalDiscount != 0) {
            Villager villager = (Villager) (Object) this;
            for (MerchantOffer offer : villager.getOffers()) {
                int discountAmount = (int) Math.round(offer.getBaseCostA().getCount() * totalDiscount);
                offer.addToSpecialPriceDiff(-discountAmount);
            }
        }
    }
}
