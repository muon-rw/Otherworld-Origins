package dev.muon.otherworldorigins.skills;

import com.seniors.justlevelingfork.JustLevelingFork;
import com.seniors.justlevelingfork.registry.RegistryAptitudes;
import com.seniors.justlevelingfork.registry.RegistrySkills;
import com.seniors.justlevelingfork.registry.skills.Skill;
import dev.muon.otherworldorigins.OtherworldOrigins;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModSkills {
    public static final DeferredRegister<Skill> SKILLS = DeferredRegister.create(RegistrySkills.SKILLS_KEY, OtherworldOrigins.MODID);

    public static final RegistryObject<Skill> REFORGING = SKILLS.register("reforging", () ->
            new Skill(
                    OtherworldOrigins.loc("reforging"),
                    RegistryAptitudes.BUILDING.get(),
                    20,
                    new ResourceLocation(JustLevelingFork.MOD_ID,"textures/skill/building/locked_24.png")
            ));

    public static void register(IEventBus eventBus) {
        SKILLS.register(eventBus);
    }
}