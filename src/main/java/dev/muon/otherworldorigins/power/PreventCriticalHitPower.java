package dev.muon.otherworldorigins.power;

import io.github.edwinmindcraft.apoli.api.configuration.NoConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;

public class PreventCriticalHitPower extends PowerFactory<NoConfiguration> {
    public PreventCriticalHitPower() {
        super(NoConfiguration.CODEC);
    }
}