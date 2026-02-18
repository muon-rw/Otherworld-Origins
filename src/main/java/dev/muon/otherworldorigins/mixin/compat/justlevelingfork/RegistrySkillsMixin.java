package dev.muon.otherworldorigins.mixin.compat.justlevelingfork;

import com.seniors.justlevelingfork.client.core.Value;
import com.seniors.justlevelingfork.client.core.ValueType;
import com.seniors.justlevelingfork.handler.HandlerCommonConfig;
import com.seniors.justlevelingfork.handler.HandlerResources;
import com.seniors.justlevelingfork.registry.RegistryAptitudes;
import com.seniors.justlevelingfork.registry.RegistrySkills;
import com.seniors.justlevelingfork.registry.skills.Skill;
import dev.muon.otherworldorigins.OtherworldOrigins;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = RegistrySkills.class, remap = false)
public class RegistrySkillsMixin {

    /**
     * @author OtherworldOrigins
     * @reason Replace Diamond Skin with 15% resistance (30% while sneaking); use our own key for reliable lang
     */
    @Overwrite
    private static Skill lambda$static$12() {
        return new Skill(
                OtherworldOrigins.loc("diamond_skin"),
                RegistryAptitudes.DEFENSE.get(),
                HandlerCommonConfig.HANDLER.instance().diamondSkinRequiredLevel,
                HandlerResources.DIAMOND_SKIN_SKILL,
                new Value(ValueType.PERCENT, 15),
                new Value(ValueType.AMPLIFIER, 0),
                new Value(ValueType.PERCENT, 30)
        );
    }
}
