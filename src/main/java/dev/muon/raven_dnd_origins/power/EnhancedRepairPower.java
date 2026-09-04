package dev.muon.raven_dnd_origins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.overgrown.apoli.condition.ItemCondition;
import dev.overgrown.apoli.data.AttributeModifierOperation;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.Attribute;

/**
 * Config-only power: {@code item_condition} gates which items qualify, {@code attribute}/
 * {@code operation}/{@code value} describe the repair-attribute bonus applied by the
 * apotheosis/anvil repair mixin.
 */
public final class EnhancedRepairPower extends PowerType<EnhancedRepairPower.Configuration> {

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ItemCondition.CODEC.fieldOf("item_condition").forGetter(Configuration::itemCondition),
            BuiltInRegistries.ATTRIBUTE.holderByNameCodec().fieldOf("attribute").forGetter(Configuration::attribute),
            AttributeModifierOperation.CODEC.fieldOf("operation").forGetter(Configuration::operation),
            Codec.FLOAT.fieldOf("value").forGetter(Configuration::value)
    ).apply(i, Configuration::new));

    @Override
    public MapCodec<Configuration> configCodec() {
        return CODEC;
    }

    public record Configuration(
            ItemCondition itemCondition,
            Holder<Attribute> attribute,
            AttributeModifierOperation operation,
            float value
    ) {
    }
}
