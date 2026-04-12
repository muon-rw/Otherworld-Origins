package dev.muon.otherworldorigins.util.spell;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.otherworldorigins.OtherworldOrigins;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.tags.ITag;
import net.minecraftforge.registries.tags.ITagManager;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Describes how to pick an Iron's Spellbooks spell and cast level for {@code cast_spell} actions.
 */
public final class SpellSelection {

    /**
     * How to pick base spell level when {@code min_rarity}/{@code max_rarity} are set. Ignored when they are absent.
     */
    public enum LevelMode implements StringRepresentable {
        LOWEST("lowest"),
        HIGHEST("highest"),
        RANDOM("random");

        public static final Codec<LevelMode> CODEC = StringRepresentable.fromEnum(LevelMode::values);

        private final String serializedName;

        LevelMode(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }

    private static final Codec<SpellSelection> OBJECT_CODEC_RAW = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.optionalFieldOf("spell").forGetter(s -> s.singleSpell),
            Codec.list(ResourceLocation.CODEC).optionalFieldOf("spells").forGetter(s -> s.spellList),
            ResourceLocation.CODEC.optionalFieldOf("spell_tag").forGetter(s -> s.spellTag),
            ResourceLocation.CODEC.optionalFieldOf("spell_school").forGetter(s -> s.spellSchool),
            SpellRarity.CODEC.optionalFieldOf("min_rarity").forGetter(s -> s.minRarity),
            SpellRarity.CODEC.optionalFieldOf("max_rarity").forGetter(s -> s.maxRarity),
            LevelMode.CODEC.optionalFieldOf("level_mode", LevelMode.LOWEST).forGetter(s -> s.levelMode),
            Codec.INT.optionalFieldOf("power_level", 1).forGetter(s -> s.powerLevel),
            Codec.INT.optionalFieldOf("level_bonus", 0).forGetter(s -> s.levelBonus)
    ).apply(instance, SpellSelection::newForObjectDecode));

    /**
     * Object form: {@code "spell": \{ ... \}} with mutually exclusive source fields.
     */
    public static final Codec<SpellSelection> OBJECT_CODEC = OBJECT_CODEC_RAW.comapFlatMap(
            SpellSelection::validateExactlyOneSource,
            Function.identity()
    );

    /**
     * Accepts a string spell id or a full object selection (nested codec; top-level {@code power_level} is merged by action configuration codecs).
     */
    public static final Codec<SpellSelection> CODEC = Codec.either(ResourceLocation.CODEC, OBJECT_CODEC)
            .xmap(
                    either -> either.map(rl -> fromLegacyStringForm(rl, 1), sel -> sel),
                    selection -> selection.encodeForActionSpellField()
            );

    public record ResolvedSpell(AbstractSpell spell, int level) {
    }

    private final Optional<ResourceLocation> singleSpell;
    private final Optional<List<ResourceLocation>> spellList;
    private final Optional<ResourceLocation> spellTag;
    private final Optional<ResourceLocation> spellSchool;

    private final Optional<SpellRarity> minRarity;
    private final Optional<SpellRarity> maxRarity;

    private final LevelMode levelMode;

    private final int powerLevel;
    private final int levelBonus;

    /**
     * When true, this selection came only from a string {@code spell} field plus optional top-level {@code power_level},
     * and should round-trip that way from {@link dev.muon.otherworldorigins.action.entity.CastSpellAction.Configuration} (or bientity).
     */
    private final boolean legacyTopLevelStringForm;

    private SpellSelection(
            Optional<ResourceLocation> singleSpell,
            Optional<List<ResourceLocation>> spellList,
            Optional<ResourceLocation> spellTag,
            Optional<ResourceLocation> spellSchool,
            Optional<SpellRarity> minRarity,
            Optional<SpellRarity> maxRarity,
            LevelMode levelMode,
            int powerLevel,
            int levelBonus,
            boolean legacyTopLevelStringForm
    ) {
        this.singleSpell = singleSpell;
        this.spellList = spellList;
        this.spellTag = spellTag;
        this.spellSchool = spellSchool;
        this.minRarity = minRarity;
        this.maxRarity = maxRarity;
        this.levelMode = levelMode;
        this.powerLevel = powerLevel;
        this.levelBonus = levelBonus;
        this.legacyTopLevelStringForm = legacyTopLevelStringForm;
    }

    private static SpellSelection newForObjectDecode(
            Optional<ResourceLocation> singleSpell,
            Optional<List<ResourceLocation>> spells,
            Optional<ResourceLocation> spellTag,
            Optional<ResourceLocation> spellSchool,
            Optional<SpellRarity> minRarity,
            Optional<SpellRarity> maxRarity,
            LevelMode levelMode,
            int powerLevel,
            int levelBonus
    ) {
        return new SpellSelection(
                singleSpell,
                spells.map(List::copyOf),
                spellTag,
                spellSchool,
                minRarity,
                maxRarity,
                levelMode,
                powerLevel,
                levelBonus,
                false
        );
    }

    private static DataResult<SpellSelection> validateExactlyOneSource(SpellSelection s) {
        int sources = 0;
        if (s.singleSpell.isPresent()) {
            sources++;
        }
        if (s.spellList.isPresent() && !s.spellList.get().isEmpty()) {
            sources++;
        }
        if (s.spellTag.isPresent()) {
            sources++;
        }
        if (s.spellSchool.isPresent()) {
            sources++;
        }
        final int sourceTotal = sources;
        if (sourceTotal != 1) {
            return DataResult.error(() -> "Spell selection must set exactly one of: spell, spells, spell_tag, spell_school (found " + sourceTotal + " sources)");
        }
        return DataResult.success(s);
    }

    public static SpellSelection fromLegacyStringForm(ResourceLocation id, int powerLevel) {
        return new SpellSelection(
                Optional.of(id),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                LevelMode.LOWEST,
                powerLevel,
                0,
                true
        );
    }

    /**
     * Encodes the {@code spell} JSON field for cast actions (string + optional top-level power vs object).
     */
    public Either<ResourceLocation, SpellSelection> encodeForActionSpellField() {
        if (legacyTopLevelStringForm && singleSpell.isPresent()) {
            return Either.left(singleSpell.get());
        }
        return Either.right(this);
    }

    /**
     * When true, emit {@code power_level} next to a string {@code spell} in the parent action codec.
     */
    public boolean shouldEmitLegacyPowerLevel() {
        return legacyTopLevelStringForm && powerLevel != 1;
    }

    public Optional<Integer> legacyPowerLevelForCodec() {
        return shouldEmitLegacyPowerLevel() ? Optional.of(powerLevel) : Optional.empty();
    }

    public @Nullable ResolvedSpell resolve(RandomSource random) {
        List<AbstractSpell> candidates = gatherCandidates();
        if (candidates.isEmpty()) {
            OtherworldOrigins.LOGGER.warn("SpellSelection.resolve: no candidate spells for selection {}", this);
            return null;
        }

        boolean rarityFilter = minRarity.isPresent() || maxRarity.isPresent();
        SpellRarity minR = minRarity.orElse(SpellRarity.COMMON);
        SpellRarity maxR = maxRarity.orElse(SpellRarity.LEGENDARY);
        if (minR.getValue() > maxR.getValue()) {
            OtherworldOrigins.LOGGER.warn("SpellSelection.resolve: min_rarity {} is above max_rarity {}", minR, maxR);
            return null;
        }

        List<Candidate> viable = new ArrayList<>();
        for (AbstractSpell spell : candidates) {
            if (!isValidSpell(spell)) {
                continue;
            }
            if (rarityFilter) {
                List<Integer> levels = levelsInRarityWindow(spell, minR, maxR);
                if (levels.isEmpty()) {
                    continue;
                }
                int base = pickBaseLevelFromWindow(levels, levelMode, random);
                viable.add(new Candidate(spell, base));
            } else {
                viable.add(new Candidate(spell, powerLevel));
            }
        }

        if (viable.isEmpty()) {
            OtherworldOrigins.LOGGER.warn("SpellSelection.resolve: no spells survived filters for selection {}", this);
            return null;
        }

        Candidate picked = viable.get(random.nextInt(viable.size()));
        int level = Mth.clamp(picked.baseLevel + levelBonus, picked.spell.getMinLevel(), picked.spell.getMaxLevel());
        return new ResolvedSpell(picked.spell, level);
    }

    private List<AbstractSpell> gatherCandidates() {
        if (singleSpell.isPresent()) {
            AbstractSpell s = SpellRegistry.getSpell(normalizeId(singleSpell.get()));
            return isValidSpell(s) ? List.of(s) : List.of();
        }
        if (spellList.isPresent()) {
            List<AbstractSpell> out = new ArrayList<>();
            for (ResourceLocation id : spellList.get()) {
                AbstractSpell s = SpellRegistry.getSpell(normalizeId(id));
                if (isValidSpell(s)) {
                    out.add(s);
                }
            }
            return out;
        }
        if (spellTag.isPresent()) {
            return spellsInTag(spellTag.get());
        }
        if (spellSchool.isPresent()) {
            SchoolType school = SchoolRegistry.getSchool(normalizeId(spellSchool.get()));
            if (school == null) {
                return List.of();
            }
            return SpellRegistry.getSpellsForSchool(school);
        }
        return List.of();
    }

    private static List<AbstractSpell> spellsInTag(ResourceLocation tagId) {
        TagKey<AbstractSpell> tagKey = TagKey.create(SpellRegistry.SPELL_REGISTRY_KEY, tagId);
        IForgeRegistry<AbstractSpell> registry = SpellRegistry.REGISTRY.get();
        ITagManager<AbstractSpell> tagManager = registry.tags();
        if (tagManager == null || !tagManager.isKnownTagName(tagKey)) {
            OtherworldOrigins.LOGGER.warn("SpellSelection: unknown or unloaded spell tag {}", tagId);
            return List.of();
        }
        ITag<AbstractSpell> tag = tagManager.getTag(tagKey);
        return tag.stream().toList();
    }

    private record Candidate(AbstractSpell spell, int baseLevel) {
    }

    private static List<Integer> levelsInRarityWindow(AbstractSpell spell, SpellRarity minR, SpellRarity maxR) {
        List<Integer> levels = new ArrayList<>();
        for (int level = spell.getMinLevel(); level <= spell.getMaxLevel(); level++) {
            int rv = spell.getRarity(level).getValue();
            if (rv >= minR.getValue() && rv <= maxR.getValue()) {
                levels.add(level);
            }
        }
        return levels;
    }

    private static int pickBaseLevelFromWindow(List<Integer> levels, LevelMode mode, RandomSource random) {
        return switch (mode) {
            case LOWEST -> levels.get(0);
            case HIGHEST -> levels.get(levels.size() - 1);
            case RANDOM -> levels.get(random.nextInt(levels.size()));
        };
    }

    /**
     * Same rule formerly applied inline in {@code CastSpellAction} / {@code CastSpellBientityAction}: spell and school
     * ids under {@code minecraft:} are resolved under {@code irons_spellbooks:}. No other callers depended on that
     * action-local path; features like {@link dev.muon.otherworldorigins.power.AllowedSpellsPower} use their own ID parsing.
     */
    private static ResourceLocation normalizeId(ResourceLocation id) {
        if ("minecraft".equals(id.getNamespace())) {
            return ResourceLocation.fromNamespaceAndPath("irons_spellbooks", id.getPath());
        }
        return id;
    }

    private static boolean isValidSpell(@Nullable AbstractSpell spell) {
        return spell != null && !"none".equals(spell.getSpellName());
    }

    @Override
    public String toString() {
        return "SpellSelection{" +
                "singleSpell=" + singleSpell +
                ", spellList=" + spellList +
                ", spellTag=" + spellTag +
                ", spellSchool=" + spellSchool +
                ", minRarity=" + minRarity +
                ", maxRarity=" + maxRarity +
                ", levelMode=" + levelMode +
                ", powerLevel=" + powerLevel +
                ", levelBonus=" + levelBonus +
                ", legacyTopLevelStringForm=" + legacyTopLevelStringForm +
                '}';
    }
}
