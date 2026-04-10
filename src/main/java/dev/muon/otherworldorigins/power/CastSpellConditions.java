package dev.muon.otherworldorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.tags.ITag;
import net.minecraftforge.registries.tags.ITagManager;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Optional filters for {@link ActionOnSpellCastPower} and {@link RecastSpellPower}.
 * Only fields present in JSON are applied (AND).
 */
public final class CastSpellConditions {

    private static final Set<CastSource> DEFAULT_SOURCES = defaultSourcesExceptCommand();

    private static Set<CastSource> defaultSourcesExceptCommand() {
        EnumSet<CastSource> set = EnumSet.allOf(CastSource.class);
        set.remove(CastSource.COMMAND);
        return Set.copyOf(set);
    }

    private static final Codec<CastType> CAST_TYPE_CODEC = Codec.STRING.comapFlatMap(
            name -> {
                try {
                    return DataResult.success(CastType.valueOf(name.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException e) {
                    return DataResult.error(() -> "Unknown cast type: " + name);
                }
            },
            t -> t.name().toLowerCase(Locale.ROOT)
    );

    private static final Codec<CastSource> CAST_SOURCE_CODEC = Codec.STRING.comapFlatMap(
            name -> {
                try {
                    return DataResult.success(CastSource.valueOf(name.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException e) {
                    return DataResult.error(() -> "Unknown cast source: " + name);
                }
            },
            s -> s.name().toLowerCase(Locale.ROOT)
    );

    private static final Codec<CastSpellConditions> CODEC_RAW = RecordCodecBuilder.create(instance -> instance.group(
            CAST_TYPE_CODEC.optionalFieldOf("cast_type").forGetter(c -> c.castTypeField),
            Codec.list(CAST_TYPE_CODEC).optionalFieldOf("cast_types").forGetter(c -> c.castTypesField),
            CAST_SOURCE_CODEC.optionalFieldOf("cast_source").forGetter(c -> c.castSourceField),
            Codec.list(CAST_SOURCE_CODEC).optionalFieldOf("cast_sources").forGetter(c -> c.castSourcesField),
            ResourceLocation.CODEC.optionalFieldOf("spell").forGetter(c -> c.spell),
            Codec.list(ResourceLocation.CODEC).optionalFieldOf("spells").forGetter(c -> c.spells),
            ResourceLocation.CODEC.optionalFieldOf("spell_tag").forGetter(c -> c.spellTag),
            ResourceLocation.CODEC.optionalFieldOf("spell_school").forGetter(c -> c.spellSchool)
    ).apply(instance, CastSpellConditions::new));

    public static final Codec<CastSpellConditions> CODEC = CODEC_RAW.comapFlatMap(
            CastSpellConditions::validate,
            Function.identity()
    );

    private final Optional<CastType> castTypeField;
    private final Optional<List<CastType>> castTypesField;
    private final Optional<CastSource> castSourceField;
    private final Optional<List<CastSource>> castSourcesField;

    private final Optional<ResourceLocation> spell;
    private final Optional<List<ResourceLocation>> spells;
    private final Optional<ResourceLocation> spellTag;
    private final Optional<ResourceLocation> spellSchool;

    private final Set<CastType> effectiveCastTypes;
    private final Set<CastSource> effectiveCastSources;

    private CastSpellConditions(
            Optional<CastType> castTypeField,
            Optional<List<CastType>> castTypesField,
            Optional<CastSource> castSourceField,
            Optional<List<CastSource>> castSourcesField,
            Optional<ResourceLocation> spell,
            Optional<List<ResourceLocation>> spells,
            Optional<ResourceLocation> spellTag,
            Optional<ResourceLocation> spellSchool
    ) {
        this.castTypeField = castTypeField;
        this.castTypesField = castTypesField;
        this.castSourceField = castSourceField;
        this.castSourcesField = castSourcesField;
        this.spell = spell;
        this.spells = spells;
        this.spellTag = spellTag;
        this.spellSchool = spellSchool;

        if (castTypeField.isPresent()) {
            this.effectiveCastTypes = Set.of(castTypeField.get());
        } else if (castTypesField.isPresent() && !castTypesField.get().isEmpty()) {
            this.effectiveCastTypes = Set.copyOf(castTypesField.get());
        } else {
            this.effectiveCastTypes = Set.copyOf(EnumSet.allOf(CastType.class));
        }

        if (castSourceField.isPresent()) {
            this.effectiveCastSources = Set.of(castSourceField.get());
        } else if (castSourcesField.isPresent() && !castSourcesField.get().isEmpty()) {
            this.effectiveCastSources = Set.copyOf(castSourcesField.get());
        } else {
            this.effectiveCastSources = DEFAULT_SOURCES;
        }
    }

    private static DataResult<CastSpellConditions> validate(CastSpellConditions c) {
        if (c.castTypeField.isPresent() && c.castTypesField.isPresent() && !c.castTypesField.get().isEmpty()) {
            return DataResult.error(() -> "Set only one of cast_type or cast_types");
        }
        if (c.castSourceField.isPresent() && c.castSourcesField.isPresent() && !c.castSourcesField.get().isEmpty()) {
            return DataResult.error(() -> "Set only one of cast_source or cast_sources");
        }
        return DataResult.success(c);
    }

    public static CastSpellConditions defaults() {
        return new CastSpellConditions(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );
    }

    public boolean matches(AbstractSpell spellEntity, CastSource source, CastType castType) {
        if (!effectiveCastTypes.contains(castType)) {
            return false;
        }
        if (!effectiveCastSources.contains(source)) {
            return false;
        }
        ResourceLocation spellRl = spellEntity.getSpellResource();
        if (spell.isPresent() && !spellIdsMatch(spellRl, spell.get())) {
            return false;
        }
        if (spells.isPresent() && !spells.get().isEmpty()) {
            ResourceLocation id = spellRl;
            boolean any = false;
            for (ResourceLocation candidate : spells.get()) {
                if (spellIdsMatch(id, candidate)) {
                    any = true;
                    break;
                }
            }
            if (!any) {
                return false;
            }
        }
        if (spellTag.isPresent() && !spellInTag(spellEntity, spellTag.get())) {
            return false;
        }
        if (spellSchool.isPresent()) {
            SchoolType school = SchoolRegistry.getSchool(normalizeId(spellSchool.get()));
            if (school == null || spellEntity.getSchoolType() != school) {
                return false;
            }
        }
        return true;
    }

    private static boolean spellInTag(AbstractSpell spellEntity, ResourceLocation tagId) {
        TagKey<AbstractSpell> tagKey = TagKey.create(SpellRegistry.SPELL_REGISTRY_KEY, normalizeId(tagId));
        IForgeRegistry<AbstractSpell> registry = SpellRegistry.REGISTRY.get();
        ITagManager<AbstractSpell> tagManager = registry.tags();
        if (tagManager == null || !tagManager.isKnownTagName(tagKey)) {
            return false;
        }
        ITag<AbstractSpell> tag = tagManager.getTag(tagKey);
        ResourceLocation id = spellEntity.getSpellResource();
        return tag.stream().anyMatch(s -> s != null && spellIdsMatch(id, s.getSpellResource()));
    }

    static boolean spellIdsMatch(ResourceLocation a, ResourceLocation b) {
        if (a.equals(b)) {
            return true;
        }
        String aStr = a.getNamespace().equals("minecraft") ? "irons_spellbooks:" + a.getPath() : a.toString();
        String bStr = b.getNamespace().equals("minecraft") ? "irons_spellbooks:" + b.getPath() : b.toString();
        return aStr.equals(bStr);
    }

    private static ResourceLocation normalizeId(ResourceLocation id) {
        if ("minecraft".equals(id.getNamespace())) {
            return ResourceLocation.fromNamespaceAndPath("irons_spellbooks", id.getPath());
        }
        return id;
    }
}
