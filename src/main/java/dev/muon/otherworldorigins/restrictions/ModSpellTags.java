package dev.muon.otherworldorigins.restrictions;

import dev.muon.otherworldorigins.OtherworldOrigins;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.tags.TagKey;

public class ModSpellTags {
    public static final TagKey<AbstractSpell> OFFENSIVE = TagKey.create(SpellRegistry.SPELL_REGISTRY_KEY, OtherworldOrigins.loc("offensive"));
    public static final TagKey<AbstractSpell> CONJURING = TagKey.create(SpellRegistry.SPELL_REGISTRY_KEY, OtherworldOrigins.loc("conjuring"));
    public static final TagKey<AbstractSpell> SUPPORT = TagKey.create(SpellRegistry.SPELL_REGISTRY_KEY, OtherworldOrigins.loc("support"));
    public static final TagKey<AbstractSpell> DEFENSIVE = TagKey.create(SpellRegistry.SPELL_REGISTRY_KEY, OtherworldOrigins.loc("defensive"));
    public static final TagKey<AbstractSpell> MELEE = TagKey.create(SpellRegistry.SPELL_REGISTRY_KEY, OtherworldOrigins.loc("melee"));
    public static final TagKey<AbstractSpell> CONTROL = TagKey.create(SpellRegistry.SPELL_REGISTRY_KEY, OtherworldOrigins.loc("control"));
    public static final TagKey<AbstractSpell> UTILITY = TagKey.create(SpellRegistry.SPELL_REGISTRY_KEY, OtherworldOrigins.loc("utility"));
    public static final TagKey<AbstractSpell> UNRESTRICTED = TagKey.create(SpellRegistry.SPELL_REGISTRY_KEY, OtherworldOrigins.loc("unrestricted"));
}
