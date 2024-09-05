package dev.muon.otherworldorigins.power.factory;

import dev.muon.otherworldorigins.power.OwnerAttributeTransferPower;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;

public class OwnerAttributeTransferPowerFactory extends PowerFactory<OwnerAttributeTransferPower> {

    public OwnerAttributeTransferPowerFactory() {
        super(OwnerAttributeTransferPower.CODEC);
    }
}