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

    public static final RegistryObject<DiveBombMarkEffect> DIVE_BOMB_MARK =
            MOB_EFFECTS.register("dive_bomb_mark", DiveBombMarkEffect::new);

    public static final RegistryObject<CuttingWordsEffect> CUTTING_WORDS =
            MOB_EFFECTS.register("cutting_words", CuttingWordsEffect::new);

    public static final RegistryObject<BattleHymnEffect> BATTLE_HYMN =
            MOB_EFFECTS.register("battle_hymn", BattleHymnEffect::new);

    public static final RegistryObject<FlourishMomentumEffect> FLOURISH_MOMENTUM =
            MOB_EFFECTS.register("flourish_momentum", FlourishMomentumEffect::new);

    public static final RegistryObject<QuiveringPalmMarkEffect> QUIVERING_PALM_MARK =
            MOB_EFFECTS.register("quivering_palm_mark", QuiveringPalmMarkEffect::new);

    public static final RegistryObject<QuiveringPalmLockoutEffect> QUIVERING_PALM_LOCKOUT =
            MOB_EFFECTS.register("quivering_palm_lockout", QuiveringPalmLockoutEffect::new);

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}
