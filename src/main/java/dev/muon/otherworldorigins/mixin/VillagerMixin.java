package dev.muon.otherworldorigins.mixin;

import dev.muon.otherworldorigins.power.ModPowers;
import dev.muon.otherworldorigins.power.TradeDiscountPower;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
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
        IPowerContainer powerContainer = ApoliAPI.getPowerContainer(player);
        if (powerContainer != null) {
            var playerPowers = powerContainer.getPowers(ModPowers.TRADE_DISCOUNT.get());
            float totalDiscount = playerPowers.stream()
                    .map(holder -> holder.value().getConfiguration())
                    .map(TradeDiscountPower.Configuration::amount)
                    .reduce(0f, Float::sum);

            if (totalDiscount != 0) {
                Villager villager = (Villager) (Object) this;
                for (MerchantOffer offer : villager.getOffers()) {
                    int discountAmount = Math.round(offer.getBaseCostA().getCount() * totalDiscount);
                    offer.addToSpecialPriceDiff(-discountAmount);
                }
            }
        }
    }
}