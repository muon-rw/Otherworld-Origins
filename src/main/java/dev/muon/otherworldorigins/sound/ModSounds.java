package dev.muon.otherworldorigins.sound;

import dev.muon.otherworldorigins.OtherworldOrigins;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, OtherworldOrigins.MODID);

    public static final RegistryObject<SoundEvent> DASH =
            SOUND_EVENTS.register("dash", () -> SoundEvent.createVariableRangeEvent(OtherworldOrigins.loc("dash")));

    public static final RegistryObject<SoundEvent> FLOURISH =
            SOUND_EVENTS.register("flourish", () -> SoundEvent.createVariableRangeEvent(OtherworldOrigins.loc("flourish")));

    private ModSounds() {}

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}
