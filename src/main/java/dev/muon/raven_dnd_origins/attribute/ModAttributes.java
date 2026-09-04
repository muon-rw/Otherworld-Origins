package dev.muon.raven_dnd_origins.attribute;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModAttributes {

    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, RavenDndOrigins.MODID);

    public static final DeferredHolder<Attribute, Attribute> HEALTH_PER_LEVEL = ATTRIBUTES.register("health_per_level",
            () -> new RangedAttribute("attribute.raven_dnd_origins.health_per_level", 0.0, 0.0, 64.0).setSyncable(true));

    public static void register(IEventBus eventBus) {
        ATTRIBUTES.register(eventBus);
    }
}
