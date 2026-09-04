package dev.muon.raven_dnd_origins.entity;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.entity.summons.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, RavenDndOrigins.MODID);

    static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> registerEntity(String name, EntityType.Builder<T> builder) {
        return ENTITY_TYPES.register(name, () -> builder.build(RavenDndOrigins.MODID + ":" + name));
    }

    public static final DeferredHolder<EntityType<?>, EntityType<SummonedIronGolem>> SUMMONED_IRON_GOLEM = registerEntity("summoned_iron_golem",
            EntityType.Builder.<SummonedIronGolem>of(SummonedIronGolem::new, MobCategory.CREATURE).sized(2.0F, 2.5F).clientTrackingRange(10));

    /**
     * Null when naturalist is absent: SummonedGrizzlyBear extends its Bear, and the constructor reference below
     * would load that superclass during this class's static init, before any mod-presence check could run.
     */
    @Nullable
    public static final DeferredHolder<EntityType<?>, EntityType<SummonedGrizzlyBear>> SUMMONED_GRIZZLY_BEAR =
            ModList.get().isLoaded("naturalist") ? registerGrizzlyBear() : null;

    private static DeferredHolder<EntityType<?>, EntityType<SummonedGrizzlyBear>> registerGrizzlyBear() {
        return registerEntity("summoned_grizzly_bear",
                EntityType.Builder.<SummonedGrizzlyBear>of(SummonedGrizzlyBear::new, MobCategory.CREATURE).sized(1.4F, 1.7F).clientTrackingRange(10));
    }

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
