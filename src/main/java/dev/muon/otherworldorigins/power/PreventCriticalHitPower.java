package dev.muon.otherworldorigins.power;

import dev.muon.otherworldorigins.OtherworldOrigins;
import io.github.apace100.apoli.power.Power;
import io.github.apace100.apoli.power.PowerType;
import io.github.apace100.apoli.power.factory.PowerFactory;
import io.github.apace100.calio.data.SerializableData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class PreventCriticalHitPower extends Power {
    public PreventCriticalHitPower(PowerType<?> type, LivingEntity entity) {
        super(type, entity);
    }

    public static PowerFactory<?> createFactory() {
        return new PowerFactory<>(
                OtherworldOrigins.loc("prevent_critical_hit"),
                new SerializableData(),
                data -> (type, entity) -> new PreventCriticalHitPower(type, entity)
        ).allowCondition();
    }
}
