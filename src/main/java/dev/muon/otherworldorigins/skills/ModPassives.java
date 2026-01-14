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
                    ResourceLocation.fromNamespaceAndPath(JustLevelingFork.MOD_ID, "textures/skill/magic/locked_0.png"),
                    AttributeRegistry.MAX_MANA.get(),
                    "A361E604-9547-E8AB-E743-62273EF1DFCA",
                    400.0,
                    2, 6, 10, 14, 18
            ));

    public static final RegistryObject<Passive> INT_SPELL_POWER = PASSIVES.register("int_spell_power", () ->
            new Passive(
                    OtherworldOrigins.loc("int_spell_power"),
                    RegistryAptitudes.INTELLIGENCE.get(),
                    ResourceLocation.fromNamespaceAndPath(JustLevelingFork.MOD_ID, "textures/skill/intelligence/locked_16.png"),
                    AttributeRegistry.SPELL_POWER.get(),
                    "C1CF34A5-DC32-B815-81C3-01AB00612506",
                    1.0,
                    4, 8, 12, 16, 20
            ));

    public static final RegistryObject<Passive> INT_COOLDOWN_REDUCTION = PASSIVES.register("int_cooldown_reduction", () ->
            new Passive(
                    OtherworldOrigins.loc("int_cooldown_reduction"),
                    RegistryAptitudes.INTELLIGENCE.get(),
                    ResourceLocation.fromNamespaceAndPath(JustLevelingFork.MOD_ID, "textures/skill/intelligence/passive_attack_speed.png"),
                    AttributeRegistry.COOLDOWN_REDUCTION.get(),
                    "96a891fe-5919-418d-8205-f50464391509",
                    0.2,
                    2, 6, 10, 14, 18
            ));

    public static void register(IEventBus eventBus) {
        PASSIVES.register(eventBus);
    }
}