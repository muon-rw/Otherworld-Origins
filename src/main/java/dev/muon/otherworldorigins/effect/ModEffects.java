package dev.muon.otherworldorigins.effect;

import dev.muon.otherworldorigins.OtherworldOrigins;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEffects {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, OtherworldOrigins.MODID);

    public static final RegistryObject<FavoredFoeEffect> FAVORED_FOE =
            MOB_EFFECTS.register("favored_foe", FavoredFoeEffect::new);

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}
