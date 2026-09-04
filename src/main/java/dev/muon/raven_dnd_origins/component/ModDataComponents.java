package dev.muon.raven_dnd_origins.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.util.ArtisanBrewNbt.Bonus;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;
import java.util.UUID;

public final class ModDataComponents {

    private static final DeferredRegister.DataComponents REGISTER =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, RavenDndOrigins.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ArtisanBrew>> ARTISAN_BREW =
            REGISTER.registerComponentType("artisan_brew", b -> b
                    .persistent(ArtisanBrew.CODEC)
                    .networkSynchronized(ArtisanBrew.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Masterwork>> MASTERWORK =
            REGISTER.registerComponentType("masterwork", b -> b
                    .persistent(Masterwork.CODEC)
                    .networkSynchronized(Masterwork.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> SOUL_OF_ARTIFICE =
            REGISTER.registerComponentType("soul_of_artifice", b -> b
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC));

    private ModDataComponents() {
    }

    public static void register(IEventBus modBus) {
        REGISTER.register(modBus);
    }

    public record ArtisanBrew(Optional<UUID> brewer, Bonus beneficial, Bonus harmful, Bonus neutral) {
        private static final Codec<Bonus> BONUS_CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.FLOAT.optionalFieldOf("duration_mult", 1.0f).forGetter(Bonus::durationMultiplier),
                Codec.INT.optionalFieldOf("amplifier_add", 0).forGetter(Bonus::amplifierAdd)
        ).apply(i, Bonus::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, Bonus> BONUS_STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.FLOAT, Bonus::durationMultiplier,
                ByteBufCodecs.VAR_INT, Bonus::amplifierAdd,
                Bonus::new);

        public static final Codec<ArtisanBrew> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.optionalFieldOf("brewer_uuid").forGetter(ArtisanBrew::brewer),
                BONUS_CODEC.optionalFieldOf("beneficial", Bonus.NONE).forGetter(ArtisanBrew::beneficial),
                BONUS_CODEC.optionalFieldOf("harmful", Bonus.NONE).forGetter(ArtisanBrew::harmful),
                BONUS_CODEC.optionalFieldOf("neutral", Bonus.NONE).forGetter(ArtisanBrew::neutral)
        ).apply(i, ArtisanBrew::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ArtisanBrew> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs::optional), ArtisanBrew::brewer,
                BONUS_STREAM_CODEC, ArtisanBrew::beneficial,
                BONUS_STREAM_CODEC, ArtisanBrew::harmful,
                BONUS_STREAM_CODEC, ArtisanBrew::neutral,
                ArtisanBrew::new);
    }

    public record Masterwork(UUID id, Holder<Attribute> attribute, AttributeModifier.Operation operation, double value) {
        public static final Codec<Masterwork> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(Masterwork::id),
                BuiltInRegistries.ATTRIBUTE.holderByNameCodec().fieldOf("attribute").forGetter(Masterwork::attribute),
                AttributeModifier.Operation.CODEC.fieldOf("operation").forGetter(Masterwork::operation),
                Codec.DOUBLE.fieldOf("value").forGetter(Masterwork::value)
        ).apply(i, Masterwork::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Masterwork> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Masterwork::id,
                ByteBufCodecs.holderRegistry(Registries.ATTRIBUTE), Masterwork::attribute,
                AttributeModifier.Operation.STREAM_CODEC, Masterwork::operation,
                ByteBufCodecs.DOUBLE, Masterwork::value,
                Masterwork::new);
    }
}
