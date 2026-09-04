package dev.muon.raven_dnd_origins.mixin;

import dev.muon.raven_dnd_origins.power.TradeDiscountPower;
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
    private void raven_dnd_origins$applyTradeDiscount(Player player, CallbackInfo ci) {
        float totalDiscount = TradeDiscountPower.getTotalDiscount(player);
        if (totalDiscount == 0) {
            return;
        }
        Villager villager = (Villager) (Object) this;
        for (MerchantOffer offer : villager.getOffers()) {
            int discountAmount = Math.round(offer.getBaseCostA().getCount() * totalDiscount);
            offer.addToSpecialPriceDiff(-discountAmount);
        }
    }
}
