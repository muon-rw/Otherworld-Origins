package dev.muon.raven_dnd_origins.sound;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, RavenDndOrigins.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> BOOST = variableRange("boost");
    public static final DeferredHolder<SoundEvent, SoundEvent> DASH = variableRange("dash");
    public static final DeferredHolder<SoundEvent, SoundEvent> DIVINE_SMITE = variableRange("divine_smite");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLOURISH = variableRange("flourish");
    public static final DeferredHolder<SoundEvent, SoundEvent> JUMP = variableRange("jump");
    public static final DeferredHolder<SoundEvent, SoundEvent> VALKYRIE_LAND = variableRange("valkyrie_land");
    public static final DeferredHolder<SoundEvent, SoundEvent> ZHH_WOO_VOOP_EARLY = variableRange("zhh_woo_voop_early");

    private ModSounds() {}

    private static DeferredHolder<SoundEvent, SoundEvent> variableRange(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(RavenDndOrigins.loc(name)));
    }

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}
