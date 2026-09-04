package dev.muon.raven_dnd_origins.spells;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSpells {
    public static final DeferredRegister<AbstractSpell> SPELLS = DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, RavenDndOrigins.MODID);

    public static void register(IEventBus eventBus) {
        SPELLS.register(eventBus);
    }

    private static DeferredHolder<AbstractSpell, AbstractSpell> registerSpell(AbstractSpell spell) {
        return SPELLS.register(spell.getSpellName(), () -> spell);
    }

    public static final DeferredHolder<AbstractSpell, AbstractSpell> SUMMON_IRON_GOLEM = registerSpell(new SummonGolemSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SUMMON_GRIZZLY_BEAR = registerSpell(new SummonGrizzlyBearSpell());

    public static final DeferredHolder<AbstractSpell, AbstractSpell> BLACK_DRAGON_BREATH = registerSpell(new DragonBreathSpells.BlackDragonBreathSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> BLUE_DRAGON_BREATH = registerSpell(new DragonBreathSpells.BlueDragonBreathSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> BRASS_DRAGON_BREATH = registerSpell(new DragonBreathSpells.BrassDragonBreathSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> BRONZE_DRAGON_BREATH = registerSpell(new DragonBreathSpells.BronzeDragonBreathSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> COPPER_DRAGON_BREATH = registerSpell(new DragonBreathSpells.CopperDragonBreathSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> GOLD_DRAGON_BREATH = registerSpell(new DragonBreathSpells.GoldDragonBreathSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> GREEN_DRAGON_BREATH = registerSpell(new DragonBreathSpells.GreenDragonBreathSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> RED_DRAGON_BREATH = registerSpell(new DragonBreathSpells.RedDragonBreathSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> SILVER_DRAGON_BREATH = registerSpell(new DragonBreathSpells.SilverDragonBreathSpell());
    public static final DeferredHolder<AbstractSpell, AbstractSpell> WHITE_DRAGON_BREATH = registerSpell(new DragonBreathSpells.WhiteDragonBreathSpell());

    public static AbstractSpell getSpell(ResourceLocation resourceLocation) {
        return SPELLS.getEntries().stream()
                .filter(entry -> entry.getId().equals(resourceLocation))
                .map(DeferredHolder::get)
                .findFirst()
                .orElse(null);
    }
}
