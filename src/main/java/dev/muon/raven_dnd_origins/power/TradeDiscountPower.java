package dev.muon.raven_dnd_origins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.world.entity.player.Player;

public final class TradeDiscountPower extends PowerType<TradeDiscountPower.Configuration> {

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.fieldOf("amount").forGetter(Configuration::amount)
    ).apply(i, Configuration::new));

    @Override
    public MapCodec<Configuration> configCodec() {
        return CODEC;
    }

    public static float getTotalDiscount(Player player) {
        float total = 0.0f;
        for (Configuration config : PowerLookup.active(player, ModPowers.TRADE_DISCOUNT, Configuration.class)) {
            total += config.amount();
        }
        return total;
    }

    public record Configuration(float amount) {
    }
}
