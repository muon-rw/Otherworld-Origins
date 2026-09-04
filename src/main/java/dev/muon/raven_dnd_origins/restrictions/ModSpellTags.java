package dev.muon.raven_dnd_origins.restrictions;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.tags.TagKey;

public class ModSpellTags {
    public static final TagKey<AbstractSpell> OFFENSIVE = TagKey.create(SpellRegistry.SPELL_REGISTRY_KEY, RavenDndOrigins.loc("offensive"));
    public static final TagKey<AbstractSpell> CONJURING = TagKey.create(SpellRegistry.SPELL_REGISTRY_KEY, RavenDndOrigins.loc("conjuring"));
    public static final TagKey<AbstractSpell> SUPPORT = TagKey.create(SpellRegistry.SPELL_REGISTRY_KEY, RavenDndOrigins.loc("support"));
    public static final TagKey<AbstractSpell> DEFENSIVE = TagKey.create(SpellRegistry.SPELL_REGISTRY_KEY, RavenDndOrigins.loc("defensive"));
    public static final TagKey<AbstractSpell> MELEE = TagKey.create(SpellRegistry.SPELL_REGISTRY_KEY, RavenDndOrigins.loc("melee"));
    public static final TagKey<AbstractSpell> CONTROL = TagKey.create(SpellRegistry.SPELL_REGISTRY_KEY, RavenDndOrigins.loc("control"));
    public static final TagKey<AbstractSpell> UTILITY = TagKey.create(SpellRegistry.SPELL_REGISTRY_KEY, RavenDndOrigins.loc("utility"));
    public static final TagKey<AbstractSpell> UNRESTRICTED = TagKey.create(SpellRegistry.SPELL_REGISTRY_KEY, RavenDndOrigins.loc("unrestricted"));
    public static final TagKey<AbstractSpell> WILD_MAGIC_SURGE = TagKey.create(SpellRegistry.SPELL_REGISTRY_KEY, RavenDndOrigins.loc("wild_magic_surge"));
}
