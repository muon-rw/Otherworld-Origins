package dev.muon.otherworldorigins.power;

import io.github.edwinmindcraft.apoli.api.configuration.NoConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;

public class PreventRepairPenaltyPower extends PowerFactory<NoConfiguration> {
    public PreventRepairPenaltyPower() {
        super(NoConfiguration.CODEC);
    }
}
