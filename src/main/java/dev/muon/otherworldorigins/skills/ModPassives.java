package dev.muon.otherworldorigins.skills;

import com.seniors.justlevelingfork.JustLevelingFork;
import com.seniors.justlevelingfork.registry.RegistryAptitudes;
import com.seniors.justlevelingfork.registry.RegistryPassives;
import com.seniors.justlevelingfork.registry.passive.Passive;
import dev.muon.otherworldorigins.OtherworldOrigins;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModPassives {
    public static final DeferredRegister<Passive> PASSIVES = DeferredRegister.create(RegistryPassives.PASSIVES_KEY, OtherworldOrigins.MODID);

    public static final RegistryObject<Passive> MAGIC_MANA = PASSIVES.register("magic_mana", () ->
            new Passive(
                    OtherworldOrigins.loc("magic_mana"),
                    RegistryAptitudes.MAGIC.get(),
                    new ResourceLocation(JustLevelingFork.MOD_ID,"textures/skill/magic/locked_0.png"),
                    AttributeRegistry.MAX_MANA.get(),
                    "A361E604-9547-E8AB-E743-62273EF1DFCA",
                    200.0,
                    2, 4, 6, 8, 10, 12, 14, 16, 18, 20
            ));

    public static final RegistryObject<Passive> INT_SPELL_POWER = PASSIVES.register("int_spell_power", () ->
            new Passive(
                    OtherworldOrigins.loc("int_spell_power"),
                    RegistryAptitudes.INTELLIGENCE.get(),
                    new ResourceLocation(JustLevelingFork.MOD_ID,"textures/skill/intelligence/locked_16.png"),
                    AttributeRegistry.SPELL_POWER.get(),
                    "C1CF34A5-DC32-B815-81C3-01AB00612506",
                    10.0,
                    2, 4, 6, 8, 10, 12, 14, 16, 18, 20
            ));

    public static void register(IEventBus eventBus) {
        PASSIVES.register(eventBus);
    }
}