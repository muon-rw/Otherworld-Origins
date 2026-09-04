package dev.muon.raven_dnd_origins.damage;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

public final class ModDamageTypes {

    public static final ResourceKey<DamageType> POISON =
            ResourceKey.create(Registries.DAMAGE_TYPE, RavenDndOrigins.loc("poison"));

    private ModDamageTypes() {
    }
}
