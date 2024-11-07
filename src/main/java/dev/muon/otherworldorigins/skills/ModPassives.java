package dev.muon.otherworldorigins.skills;

import com.seniors.justlevelingfork.handler.HandlerCommonConfig;
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

    public static final RegistryObject<Passive> INTELLIGENCE_MANA = PASSIVES.register("intelligence_mana", () ->
            new Passive(
                    new ResourceLocation(OtherworldOrigins.MODID, "intelligence_mana"),
                    RegistryAptitudes.INTELLIGENCE.get(),
                    new ResourceLocation(OtherworldOrigins.MODID, "textures/skills/intelligence/mana_passive.png"),
                    AttributeRegistry.MAX_MANA.get(),
                    "A361E604-9547-E8AB-E743-62273EF1DFCA",
                    200.0,
                    2, 4, 6, 8, 10, 12, 14, 16, 18, 20
            ));

    public static final RegistryObject<Passive> MAGIC_SPELL_POWER = PASSIVES.register("magic_spell_power", () ->
            new Passive(
                    new ResourceLocation(OtherworldOrigins.MODID, "magic_spell_power"),
                    RegistryAptitudes.MAGIC.get(),
                    new ResourceLocation(OtherworldOrigins.MODID, "textures/skills/magic/spell_power_passive.png"),
                    AttributeRegistry.SPELL_POWER.get(),
                    "C1CF34A5-DC32-B815-81C3-01AB00612506",
                    HandlerCommonConfig.HANDLER.instance().projectileDamageValue,
                    HandlerCommonConfig.HANDLER.instance().projectileDamagePassiveLevels
            ));

    public static void register(IEventBus eventBus) {
        PASSIVES.register(eventBus);
    }
}