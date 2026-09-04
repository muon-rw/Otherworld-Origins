package dev.muon.raven_dnd_origins.effect;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEffects {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, RavenDndOrigins.MODID);

    public static final DeferredHolder<MobEffect, FavoredFoeEffect> FAVORED_FOE =
            MOB_EFFECTS.register("favored_foe", FavoredFoeEffect::new);

    public static final DeferredHolder<MobEffect, DiveBombMarkEffect> DIVE_BOMB_MARK =
            MOB_EFFECTS.register("dive_bomb_mark", DiveBombMarkEffect::new);

    public static final DeferredHolder<MobEffect, CuttingWordsEffect> CUTTING_WORDS =
            MOB_EFFECTS.register("cutting_words", CuttingWordsEffect::new);

    public static final DeferredHolder<MobEffect, BattleHymnEffect> BATTLE_HYMN =
            MOB_EFFECTS.register("battle_hymn", BattleHymnEffect::new);

    public static final DeferredHolder<MobEffect, FlourishMomentumEffect> FLOURISH_MOMENTUM =
            MOB_EFFECTS.register("flourish_momentum", FlourishMomentumEffect::new);

    public static final DeferredHolder<MobEffect, QuiveringPalmMarkEffect> QUIVERING_PALM_MARK =
            MOB_EFFECTS.register("quivering_palm_mark", QuiveringPalmMarkEffect::new);

    public static final DeferredHolder<MobEffect, QuiveringPalmLockoutEffect> QUIVERING_PALM_LOCKOUT =
            MOB_EFFECTS.register("quivering_palm_lockout", QuiveringPalmLockoutEffect::new);

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}
