package dev.muon.otherworldorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.apace100.calio.data.SerializableDataTypes;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredItemCondition;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class EnhancedRepairPower extends PowerFactory<EnhancedRepairPower.Configuration> {

    public EnhancedRepairPower() {
        super(Configuration.CODEC);
    }

    public record Configuration(
            Holder<ConfiguredItemCondition<?, ?>> itemCondition,
            Attribute attribute,
            AttributeModifier.Operation operation,
            float value
    ) implements IDynamicFeatureConfiguration {

        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ConfiguredItemCondition.HOLDER.fieldOf("item_condition").forGetter(Configuration::itemCondition),
                SerializableDataTypes.ATTRIBUTE.fieldOf("attribute").forGetter(Configuration::attribute),
                SerializableDataTypes.MODIFIER_OPERATION.fieldOf("operation").forGetter(Configuration::operation),
                Codec.FLOAT.fieldOf("value").forGetter(Configuration::value)
        ).apply(instance, Configuration::new));

        @Override
        public boolean isConfigurationValid() {
            return true;
        }
    }
}
