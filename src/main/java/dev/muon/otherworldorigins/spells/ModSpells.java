package dev.muon.otherworldorigins.spells;

import dev.muon.otherworldorigins.OtherworldOrigins;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModSpells {
    private static final DeferredRegister<AbstractSpell> SPELLS = DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, OtherworldOrigins.MODID);

    public static void register(IEventBus eventBus) {
        SPELLS.register(eventBus);
    }

    private static RegistryObject<AbstractSpell> registerSpell(AbstractSpell spell) {
        return SPELLS.register(spell.getSpellName(), () -> spell);
    }

    public static final RegistryObject<AbstractSpell> BLACK_DRAGON_BREATH = registerSpell(new DragonBreathSpells.BlackDragonBreathSpell());
    public static final RegistryObject<AbstractSpell> BLUE_DRAGON_BREATH = registerSpell(new DragonBreathSpells.BlueDragonBreathSpell());
    public static final RegistryObject<AbstractSpell> BRASS_DRAGON_BREATH = registerSpell(new DragonBreathSpells.BrassDragonBreathSpell());
    public static final RegistryObject<AbstractSpell> BRONZE_DRAGON_BREATH = registerSpell(new DragonBreathSpells.BronzeDragonBreathSpell());
    public static final RegistryObject<AbstractSpell> COPPER_DRAGON_BREATH = registerSpell(new DragonBreathSpells.CopperDragonBreathSpell());
    public static final RegistryObject<AbstractSpell> GOLD_DRAGON_BREATH = registerSpell(new DragonBreathSpells.GoldDragonBreathSpell());
    public static final RegistryObject<AbstractSpell> GREEN_DRAGON_BREATH = registerSpell(new DragonBreathSpells.GreenDragonBreathSpell());
    public static final RegistryObject<AbstractSpell> RED_DRAGON_BREATH = registerSpell(new DragonBreathSpells.RedDragonBreathSpell());
    public static final RegistryObject<AbstractSpell> SILVER_DRAGON_BREATH = registerSpell(new DragonBreathSpells.SilverDragonBreathSpell());
    public static final RegistryObject<AbstractSpell> WHITE_DRAGON_BREATH = registerSpell(new DragonBreathSpells.WhiteDragonBreathSpell());

    public static AbstractSpell getSpell(ResourceLocation resourceLocation) {
        return SPELLS.getEntries().stream()
                .filter(entry -> entry.getId().equals(resourceLocation))
                .map(RegistryObject::get)
                .findFirst()
                .orElse(null);
    }
}