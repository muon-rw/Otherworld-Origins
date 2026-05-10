package dev.muon.otherworldorigins.damage;

import dev.muon.otherworldorigins.OtherworldOrigins;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

public final class ModDamageTypes {

    public static final ResourceKey<DamageType> POISON =
            ResourceKey.create(Registries.DAMAGE_TYPE, OtherworldOrigins.loc("poison"));

    private ModDamageTypes() {
    }
}
