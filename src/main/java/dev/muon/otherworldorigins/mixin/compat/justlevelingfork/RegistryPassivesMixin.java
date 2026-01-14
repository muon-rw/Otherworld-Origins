package dev.muon.otherworldorigins.mixin.compat.justlevelingfork;

import com.seniors.justlevelingfork.handler.HandlerCommonConfig;
import com.seniors.justlevelingfork.handler.HandlerResources;
import com.seniors.justlevelingfork.registry.RegistryAptitudes;
import com.seniors.justlevelingfork.registry.RegistryPassives;
import com.seniors.justlevelingfork.registry.passive.Passive;
import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.attribute.ModAttributes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = RegistryPassives.class, remap = false)
public class RegistryPassivesMixin {

    /**
     * @author OtherworldOrigins
     * @reason Replace MAX_HEALTH passive with custom health_per_level attribute
     */
    @Overwrite
    private static Passive lambda$static$3() {
        return new Passive(
                OtherworldOrigins.loc("health_per_level"),
                RegistryAptitudes.CONSTITUTION.get(),
                HandlerResources.create("textures/skill/constitution/passive_max_health.png"),
                ModAttributes.HEALTH_PER_LEVEL.get(),
                "96a891fe-5919-418d-8205-f50464391502",
                5.0f,
                4, 8, 12, 16, 20
        );
    }

    /**
     * @author OtherworldOrigins
     * @reason Change attack speed passive from Intelligence to Dexterity
     */
    @Overwrite
    private static Passive lambda$static$9() {
        return new Passive(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("justlevelingfork", "attack_speed"),
                RegistryAptitudes.DEXTERITY.get(),
                HandlerResources.create("textures/skill/intelligence/passive_attack_speed.png"),
                Attributes.ATTACK_SPEED,
                "96a891fe-5919-418d-8205-f50464391508",
                HandlerCommonConfig.HANDLER.instance().attackSpeedValue,
                HandlerCommonConfig.HANDLER.instance().attackSpeedPassiveLevels
        );
    }
}
