package dev.muon.otherworldorigins.entity;

import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.entity.summons.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;


@Mod.EventBusSubscriber(modid = OtherworldOrigins.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, OtherworldOrigins.MODID);
    static <T extends Entity> RegistryObject<EntityType<T>> registerEntity(String name, EntityType.Builder<T> builder) {
        return ENTITY_TYPES.register(name, () -> builder.build(OtherworldOrigins.MODID + ":" + name));
    }

    public static final RegistryObject<EntityType<SummonedIronGolem>> SUMMONED_IRON_GOLEM = registerEntity("summoned_iron_golem",
            EntityType.Builder.<SummonedIronGolem>of(SummonedIronGolem::new, MobCategory.CREATURE).sized(2.0F, 2.5F).clientTrackingRange(10));
    public static final RegistryObject<EntityType<SummonedGrizzlyBear>> SUMMONED_GRIZZLY_BEAR = registerEntity("summoned_grizzly_bear",
            EntityType.Builder.<SummonedGrizzlyBear>of(SummonedGrizzlyBear::new, MobCategory.CREATURE).sized(2.0F, 2.5F).clientTrackingRange(10));


    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }

}
