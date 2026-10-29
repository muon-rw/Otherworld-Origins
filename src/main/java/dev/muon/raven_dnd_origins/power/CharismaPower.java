package dev.muon.raven_dnd_origins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.world.entity.Entity;

public final class CharismaPower extends PowerType<CharismaPower.Configuration> {

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.fieldOf("amount").forGetter(Configuration::amount)
    ).apply(i, Configuration::new));

    @Override
    public MapCodec<Configuration> configCodec() {
        return CODEC;
    }

    public static float totalCharisma(Entity entity) {
        float total = 0f;
        for (Configuration cfg : PowerLookup.active(entity, ModPowers.CHARISMA, Configuration.class)) {
            total += cfg.amount();
        }
        return total;
    }

    public record Configuration(float amount) {
    }
}
