package dev.muon.otherworldorigins.power;

import dev.muon.otherworldorigins.OtherworldOrigins;
import io.github.apace100.apoli.power.Power;
import io.github.apace100.apoli.power.PowerType;
import io.github.apace100.apoli.power.factory.PowerFactory;
import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.calio.data.SerializableDataTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class ModifyStatusEffectCategoryPower extends Power {
    private final boolean affectBeneficial;
    private final boolean affectHarmful;
    private final boolean affectNeutral;
    private final float amount;

    public ModifyStatusEffectCategoryPower(PowerType<?> type, LivingEntity entity,
                                          boolean affectBeneficial,
                                          boolean affectHarmful,
                                          boolean affectNeutral,
                                          float amount) {
        super(type, entity);
        this.affectBeneficial = affectBeneficial;
        this.affectHarmful = affectHarmful;
        this.affectNeutral = affectNeutral;
        this.amount = amount;
    }

    public boolean doesApply(MobEffect effect) {
        MobEffectCategory category = effect.getCategory();
        return (affectBeneficial && category == MobEffectCategory.BENEFICIAL) ||
                (affectHarmful && category == MobEffectCategory.HARMFUL) ||
                (affectNeutral && category == MobEffectCategory.NEUTRAL);
    }

    public float getAmount() {
        return amount;
    }

    public static PowerFactory<?> createFactory() {
        return new PowerFactory<>(
                OtherworldOrigins.loc("modify_status_effect_category"),
                new SerializableData()
                        .add("affect_beneficial", SerializableDataTypes.BOOLEAN)
                        .add("affect_harmful", SerializableDataTypes.BOOLEAN)
                        .add("affect_neutral", SerializableDataTypes.BOOLEAN)
                        .add("amount", SerializableDataTypes.FLOAT),
                data -> (type, entity) -> new ModifyStatusEffectCategoryPower(
                        type,
                        entity,
                        data.getBoolean("affect_beneficial"),
                        data.getBoolean("affect_harmful"),
                        data.getBoolean("affect_neutral"),
                        data.getFloat("amount")
                )
        ).allowCondition();
    }
}
