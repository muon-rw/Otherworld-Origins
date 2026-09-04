package dev.muon.raven_dnd_origins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.raven_core.leveling.event.AptitudeChangedEvent;
import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.util.LeveledScaling;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.codec.IdCodecs;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.data.Expression;
import dev.overgrown.apoli.data.HudRender;
import dev.overgrown.apoli.data.expr.ExprVars;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.PowerContainer;
import dev.overgrown.apoli.power.builtin.ResourcePower;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * A resource whose maximum scales with character level (or a Just Leveling aptitude tier). The mod's
 * legacy fields decode into an Apoli {@link ResourcePower.Cfg} with an expression-backed maximum, so
 * the stock resource plumbing (HUD bar, {@code origins:resource} actions and conditions, persistence,
 * boundary actions) applies unchanged.
 */
@EventBusSubscriber(modid = RavenDndOrigins.MODID)
public class LeveledResourcePower extends ResourcePower {

    private static final String CHARACTER_LEVEL_VAR = "raven_character_level";
    private static final Set<ResourceLocation> REGISTERED_APTITUDE_VARS = Collections.synchronizedSet(new HashSet<>());

    // ResourcePower.Cfg has no field to carry restore_on_levelup, so it is tracked beside the decoded config.
    // Keys are weak and compared by identity: every reload and every client power sync decodes a fresh Cfg that
    // is equal to, but distinct from, the previous one, so value keys would strand the flag on a dead instance.
    private static final ReferenceQueue<ResourcePower.Cfg> COLLECTED_CFGS = new ReferenceQueue<>();
    private static final Set<CfgIdentity> RESTORE_ON_LEVELUP = Collections.synchronizedSet(new HashSet<>());

    static {
        ExprVars.register(CHARACTER_LEVEL_VAR, (entity, container, level, value) ->
                LeveledScaling.levelForScaling(entity, Optional.empty()));
    }

    public record SteppedMax(float start, float increment, int gap, Optional<Integer> maxSteps) {
        public SteppedMax {
            if (gap < 1) {
                throw new IllegalArgumentException("stepped_max.gap must be >= 1");
            }
            if (maxSteps.isPresent() && maxSteps.get() < 0) {
                throw new IllegalArgumentException("stepped_max.max_steps must be >= 0 when present");
            }
        }

        public static final Codec<SteppedMax> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.fieldOf("start").forGetter(SteppedMax::start),
                Codec.FLOAT.fieldOf("increment").forGetter(SteppedMax::increment),
                Codec.INT.fieldOf("gap")
                        .validate(gap -> gap >= 1
                                ? DataResult.success(gap)
                                : DataResult.error(() -> "stepped_max.gap must be >= 1"))
                        .forGetter(SteppedMax::gap),
                Codec.INT.optionalFieldOf("max_steps")
                        .validate(steps -> steps.isEmpty() || steps.get() >= 0
                                ? DataResult.success(steps)
                                : DataResult.error(() -> "stepped_max.max_steps must be >= 0 when present"))
                        .forGetter(SteppedMax::maxSteps)
        ).apply(instance, SteppedMax::new));

        String toExpression(String levelVar) {
            String steps = "floor(max(" + levelVar + ", 0) / " + num(gap) + ")";
            if (maxSteps.isPresent()) {
                steps = "min(" + steps + ", " + num(maxSteps.get()) + ")";
            }
            return "round(" + num(start) + " + " + num(increment) + " * " + steps + ")";
        }
    }

    public record LeveledMax(float base, float perLevel) {
        String toExpression(String levelVar) {
            return num(base) + " + " + num(perLevel) + " * " + levelVar;
        }
    }

    private record Fields(HudRender hudRender, Optional<Integer> startValue, int min, Optional<Integer> staticMax,
                          Optional<Float> maxBase, Optional<Float> maxPerLevel, Optional<SteppedMax> steppedMax,
                          boolean restoreOnLevelup, Optional<EntityAction> minAction, Optional<EntityAction> maxAction,
                          Optional<ResourceLocation> aptitude) {
    }

    private static final MapCodec<ResourcePower.Cfg> CODEC = RecordCodecBuilder.<Fields>mapCodec(instance -> instance.group(
            HudRender.CODEC.optionalFieldOf("hud_render", HudRender.DONT_RENDER).forGetter(Fields::hudRender),
            Codec.INT.optionalFieldOf("start_value").forGetter(Fields::startValue),
            Codec.INT.fieldOf("min").forGetter(Fields::min),
            Codec.INT.optionalFieldOf("max").forGetter(Fields::staticMax),
            Codec.FLOAT.optionalFieldOf("max_base").forGetter(Fields::maxBase),
            Codec.FLOAT.optionalFieldOf("max_per_level").forGetter(Fields::maxPerLevel),
            SteppedMax.CODEC.optionalFieldOf("stepped_max").forGetter(Fields::steppedMax),
            Codec.BOOL.optionalFieldOf("restore_on_levelup", false).forGetter(Fields::restoreOnLevelup),
            LoggedOptionalField.of("min_action", EntityAction.CODEC).forGetter(Fields::minAction),
            LoggedOptionalField.of("max_action", EntityAction.CODEC).forGetter(Fields::maxAction),
            IdCodecs.ID.optionalFieldOf("aptitude").forGetter(Fields::aptitude)
    ).apply(instance, Fields::new)).flatXmap(LeveledResourcePower::build, LeveledResourcePower::toFields);

    @Override
    public MapCodec<ResourcePower.Cfg> configCodec() {
        return CODEC;
    }

    private static DataResult<ResourcePower.Cfg> build(Fields fields) {
        if (fields.maxBase().isPresent() ^ fields.maxPerLevel().isPresent()) {
            return DataResult.error(() -> "When using linear level-based max, both 'max_base' and 'max_per_level' must be provided");
        }
        boolean hasLinear = fields.maxBase().isPresent() && fields.maxPerLevel().isPresent();
        int modeCount = (fields.staticMax().isPresent() ? 1 : 0) + (hasLinear ? 1 : 0) + (fields.steppedMax().isPresent() ? 1 : 0);
        if (modeCount != 1) {
            return DataResult.error(() -> "Provide exactly one of: 'max', ('max_base' + 'max_per_level'), or 'stepped_max'");
        }
        if (!LeveledScaling.isValidAptitudeReference(fields.aptitude())) {
            return DataResult.error(() -> "Unknown aptitude: " + fields.aptitude().orElseThrow());
        }

        Expression max;
        if (fields.staticMax().isPresent()) {
            max = Expression.constant(fields.staticMax().get());
        } else {
            String levelVar = levelVar(fields.aptitude());
            max = Expression.of(hasLinear
                    ? new LeveledMax(fields.maxBase().get(), fields.maxPerLevel().get()).toExpression(levelVar)
                    : fields.steppedMax().orElseThrow().toExpression(levelVar));
        }

        ResourcePower.Cfg cfg = new ResourcePower.Cfg(
                Optional.of(Expression.constant(fields.min())),
                Optional.of(max),
                fields.startValue().map(value -> Expression.constant(value.doubleValue())),
                fields.hudRender(),
                true,
                false,
                fields.minAction(),
                fields.maxAction(),
                true,
                1);
        if (fields.restoreOnLevelup()) {
            markRestoresOnLevelup(cfg);
        }
        return DataResult.success(cfg);
    }

    private static DataResult<Fields> toFields(ResourcePower.Cfg cfg) {
        return DataResult.success(new Fields(cfg.hudRender(), Optional.empty(), 0, Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), false, cfg.minAction(), cfg.maxAction(), Optional.empty()));
    }

    @SubscribeEvent
    public static void refillRestoringResources(AptitudeChangedEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || event.getNewLevel() == event.getOldLevel()) {
            return;
        }
        PowerContainer container = PowerContainer.of(player);
        if (container == null) return;
        for (ResourceLocation powerId : container.powersOfType(ModPowers.LEVELED_RESOURCE)) {
            Power power = ApoliPowers.get(powerId);
            if (power == null || !(power.config() instanceof ResourcePower.Cfg cfg)) continue;
            if (!restoresOnLevelup(cfg)) continue;
            OptionalInt max = ResourcePower.boundOf(container, powerId, true);
            if (max.isPresent()) {
                ResourcePower.writeValue(container, powerId, max.getAsInt());
            }
        }
    }

    private static void markRestoresOnLevelup(ResourcePower.Cfg cfg) {
        dropCollectedCfgs();
        RESTORE_ON_LEVELUP.add(new CfgIdentity(cfg, COLLECTED_CFGS));
    }

    private static boolean restoresOnLevelup(ResourcePower.Cfg cfg) {
        return RESTORE_ON_LEVELUP.contains(new CfgIdentity(cfg, null));
    }

    private static void dropCollectedCfgs() {
        for (Reference<? extends ResourcePower.Cfg> collected; (collected = COLLECTED_CFGS.poll()) != null; ) {
            RESTORE_ON_LEVELUP.remove((CfgIdentity) collected);
        }
    }

    private static final class CfgIdentity extends WeakReference<ResourcePower.Cfg> {
        private final int hash;

        CfgIdentity(ResourcePower.Cfg cfg, ReferenceQueue<ResourcePower.Cfg> queue) {
            super(cfg, queue);
            this.hash = System.identityHashCode(cfg);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            ResourcePower.Cfg cfg = get();
            return cfg != null && other instanceof CfgIdentity key && cfg == key.get();
        }
    }

    private static String levelVar(Optional<ResourceLocation> aptitude) {
        if (aptitude.isEmpty()) return CHARACTER_LEVEL_VAR;
        ResourceLocation id = aptitude.get();
        String name = "raven_aptitude_level_" + id.getNamespace() + "_" + id.getPath().replace('/', '_');
        if (REGISTERED_APTITUDE_VARS.add(id)) {
            ExprVars.register(name, (entity, container, level, value) ->
                    LeveledScaling.levelForScaling(entity, Optional.of(id)));
        }
        return name;
    }

    // Parenthesised so a negative literal never merges into the preceding operator.
    private static String num(double value) {
        String literal = value == Math.rint(value) && !Double.isInfinite(value)
                ? Long.toString((long) value)
                : String.format(Locale.ROOT, "%s", value);
        return value < 0 ? "(" + literal + ")" : literal;
    }
}
