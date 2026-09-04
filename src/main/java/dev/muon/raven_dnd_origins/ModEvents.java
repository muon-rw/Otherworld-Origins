package dev.muon.raven_dnd_origins;

import dev.muon.raven_dnd_origins.attribute.ModAttributes;
import dev.muon.raven_dnd_origins.entity.ModEntities;
import dev.muon.raven_dnd_origins.entity.summons.SummonedGrizzlyBear;
import dev.muon.raven_dnd_origins.entity.summons.SummonedIronGolem;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;

@EventBusSubscriber(modid = RavenDndOrigins.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModEvents {
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.SUMMONED_IRON_GOLEM.get(), SummonedIronGolem.createAttributes().build());
        if (ModEntities.SUMMONED_GRIZZLY_BEAR != null) {
            event.put(ModEntities.SUMMONED_GRIZZLY_BEAR.get(), SummonedGrizzlyBear.createAttributes().build());
        }
    }

    @SubscribeEvent
    public static void modifyEntityAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, ModAttributes.HEALTH_PER_LEVEL);
    }
}
