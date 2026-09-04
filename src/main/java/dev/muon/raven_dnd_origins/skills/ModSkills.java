package dev.muon.raven_dnd_origins.skills;

import com.seniors.justlevelingfork.JustLevelingFork;
import com.seniors.justlevelingfork.registry.RegistryAptitudes;
import com.seniors.justlevelingfork.registry.RegistrySkills;
import com.seniors.justlevelingfork.registry.skills.Skill;
import dev.muon.raven_dnd_origins.RavenDndOrigins;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSkills {
    public static final DeferredRegister<Skill> SKILLS = DeferredRegister.create(RegistrySkills.SKILLS_KEY, RavenDndOrigins.MODID);

    public static final DeferredHolder<Skill, Skill> REFORGING = SKILLS.register("reforging", () ->
            new Skill(
                    RavenDndOrigins.loc("reforging"),
                    RegistryAptitudes.BUILDING.get(),
                    20,
                    ResourceLocation.fromNamespaceAndPath(JustLevelingFork.MOD_ID, "textures/skill/building/locked_24.png")
            ));

    public static final DeferredHolder<Skill, Skill> WISDOM = SKILLS.register("wisdom", () ->
            new Skill(
                    RavenDndOrigins.loc("wisdom"),
                    RegistryAptitudes.MAGIC.get(),
                    20,
                    ResourceLocation.fromNamespaceAndPath(JustLevelingFork.MOD_ID, "textures/skill/intelligence/locked_24.png")
            ));

    public static void register(IEventBus eventBus) {
        SKILLS.register(eventBus);
    }
}
