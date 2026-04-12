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

    public static final RegistryObject<SoundEvent> BOOST =
            SOUND_EVENTS.register("boost", () -> SoundEvent.createVariableRangeEvent(OtherworldOrigins.loc("boost")));

    public static final RegistryObject<SoundEvent> DASH =
            SOUND_EVENTS.register("dash", () -> SoundEvent.createVariableRangeEvent(OtherworldOrigins.loc("dash")));

    public static final RegistryObject<SoundEvent> DIVINE_SMITE =
            SOUND_EVENTS.register("divine_smite", () -> SoundEvent.createVariableRangeEvent(OtherworldOrigins.loc("divine_smite")));

    public static final RegistryObject<SoundEvent> FLOURISH =
            SOUND_EVENTS.register("flourish", () -> SoundEvent.createVariableRangeEvent(OtherworldOrigins.loc("flourish")));

    public static final RegistryObject<SoundEvent> JUMP =
            SOUND_EVENTS.register("jump", () -> SoundEvent.createVariableRangeEvent(OtherworldOrigins.loc("jump")));

    public static final RegistryObject<SoundEvent> VALKYRIE_LAND =
            SOUND_EVENTS.register("valkyrie_land", () -> SoundEvent.createVariableRangeEvent(OtherworldOrigins.loc("valkyrie_land")));

    public static final RegistryObject<SoundEvent> ZHH_WOO_VOOP_EARLY =
            SOUND_EVENTS.register("zhh_woo_voop_early", () -> SoundEvent.createVariableRangeEvent(OtherworldOrigins.loc("zhh_woo_voop_early")));

    private ModSounds() {}

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}
