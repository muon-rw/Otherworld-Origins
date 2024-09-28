package dev.muon.otherworldorigins.power;

import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.power.configuration.InnateAptitudeBonusConfiguration;
import dev.muon.otherworldorigins.power.factory.OwnerAttributeTransferPowerFactory;
import dev.muon.otherworldorigins.power.factory.InnateAptitudeBonusPowerFactory;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import io.github.edwinmindcraft.apoli.api.registry.ApoliRegistries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModPowers {
    public static final DeferredRegister<PowerFactory<?>> POWER_FACTORIES = DeferredRegister.create(ApoliRegistries.POWER_FACTORY_KEY, OtherworldOrigins.MODID);

    public static final RegistryObject<PixieWingsPower> PIXIE_WINGS = POWER_FACTORIES.register("pixie_wings", PixieWingsPower::new);
    public static final RegistryObject<PowerFactory<OwnerAttributeTransferPower>> OWNER_ATTRIBUTE_TRANSFER = POWER_FACTORIES.register("owner_attribute_transfer", OwnerAttributeTransferPowerFactory::new);

    public static final RegistryObject<PowerFactory<InnateAptitudeBonusConfiguration>> INNATE_APTITUDE_BONUS = POWER_FACTORIES.register("innate_aptitude_bonus", InnateAptitudeBonusPowerFactory::new);

    private static <T extends PowerFactory<?>> RegistryObject<T> registerConditional(String name, Supplier<T> factory, String modId) {
        if (ModList.get().isLoaded(modId)) {
            return POWER_FACTORIES.register(name, factory);
        }
        return null;
    }

    public static void register(IEventBus eventBus) {
        POWER_FACTORIES.register(eventBus);
    }
}