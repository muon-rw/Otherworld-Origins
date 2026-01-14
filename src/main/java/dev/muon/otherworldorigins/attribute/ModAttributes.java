package dev.muon.otherworldorigins.attribute;

import dev.muon.otherworldorigins.OtherworldOrigins;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModAttributes {

    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, OtherworldOrigins.MODID);

    public static final RegistryObject<Attribute> HEALTH_PER_LEVEL = ATTRIBUTES.register("health_per_level",
            () -> new RangedAttribute("attribute.otherworldorigins.health_per_level", 0.0, 0.0, 64.0).setSyncable(true));

    public static void register(IEventBus eventBus) {
        ATTRIBUTES.register(eventBus);
    }
}
