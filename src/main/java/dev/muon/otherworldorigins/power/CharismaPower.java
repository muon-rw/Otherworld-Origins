package dev.muon.otherworldorigins.power;

import dev.muon.otherworldorigins.OtherworldOrigins;
import io.github.apace100.apoli.power.PowerType;
import io.github.apace100.apoli.power.ValueModifyingPower;
import io.github.apace100.apoli.power.factory.PowerFactory;
import io.github.apace100.apoli.util.modifier.Modifier;
import io.github.apace100.calio.data.SerializableData;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Objects;

public class CharismaPower extends ValueModifyingPower {

    public CharismaPower(PowerType<?> type, LivingEntity entity) {
        super(type, entity);
    }

    public static PowerFactory<?> createFactory() {
        return new PowerFactory<>(
                OtherworldOrigins.loc("charisma"),
                new SerializableData()
                        .add("modifier", Modifier.DATA_TYPE, null)
                        .add("modifiers", Modifier.LIST_TYPE, null),
                data -> (type, entity) -> {
                    CharismaPower power = new CharismaPower(type, entity);
                    Objects.requireNonNull(power);
                    data.ifPresent("modifier", power::addModifier);
                    data.ifPresent("modifiers", (List<Modifier> mods) -> {
                        Objects.requireNonNull(power);
                        mods.forEach(power::addModifier);
                    });
                    return power;
                }
        ).allowCondition();
    }
}
