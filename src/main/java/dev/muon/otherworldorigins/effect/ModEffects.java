package dev.muon.otherworldorigins.effect;

import io.redspace.ironsspellbooks.effect.SummonTimer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModEffects {
    public ModEffects() {
    }
    public static final DeferredRegister<MobEffect> MOB_EFFECTS;
    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }

    public static final RegistryObject<SummonTimer> GOLEM_TIMER;
    public static final RegistryObject<SummonTimer> BEAST_TIMER;

    static {
        MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, "otherworldorigins");
        GOLEM_TIMER = MOB_EFFECTS.register("golem_timer", () -> new SummonTimer(MobEffectCategory.BENEFICIAL, 12495141));
        BEAST_TIMER = MOB_EFFECTS.register("beast_timer", () -> new SummonTimer(MobEffectCategory.BENEFICIAL, 12495141));
    }
}
