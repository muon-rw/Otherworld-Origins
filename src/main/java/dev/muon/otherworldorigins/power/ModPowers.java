package dev.muon.otherworldorigins.power;

import dev.muon.otherworldorigins.OtherworldOrigins;
import io.github.apace100.apoli.power.factory.PowerFactory;
import io.github.apace100.apoli.registry.ApoliRegistries;
import net.minecraft.core.Registry;
import net.minecraftforge.fml.ModList;

public class ModPowers {
    public static void register() {
        registerPowerFactory(InnateAptitudeBonusPower.createFactory());
        registerPowerFactory(ActionOnLevelupPower.createFactory());
        registerPowerFactory(TradeDiscountPower.createFactory());
        registerPowerFactory(ModifyThirstExhaustionPower.createFactory());
        registerPowerFactory(CharismaPower.createFactory());
        registerPowerFactory(LeveledAttributePower.createFactory());
        registerPowerFactory(ModifyStatusEffectCategoryPower.createFactory());
        registerPowerFactory(ModifyEnchantmentCostPower.createFactory());
        registerPowerFactory(PreventCriticalHitPower.createFactory());
        registerPowerFactory(ModifyCriticalHitPower.createFactory());
        registerPowerFactory(ModifyDurabilityChangePower.createFactory());
    }

    public static void registerPowerFactory(PowerFactory<?> serializer) {
        Registry.register(ApoliRegistries.POWER_FACTORY, serializer.getSerializerId(), serializer);
    }
}
