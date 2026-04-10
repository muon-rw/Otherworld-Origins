package dev.muon.otherworldorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.otherworld.leveling.LevelingUtils;
import dev.muon.otherworld.leveling.event.AptitudeChangedEvent;
import dev.muon.otherworldorigins.OtherworldOrigins;
import io.github.apace100.apoli.util.HudRender;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredEntityAction;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.apoli.api.power.configuration.power.IHudRenderedVariableIntPowerConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.power.HudRenderedVariableIntPowerFactory;
import io.github.edwinmindcraft.calio.api.network.CalioCodecHelper;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = OtherworldOrigins.MODID)
public class LeveledResourcePower extends HudRenderedVariableIntPowerFactory.Simple<LeveledResourcePower.Configuration> {

    public LeveledResourcePower() {
        super(Configuration.CODEC);
    }

    @Override
    public int assign(ConfiguredPower<Configuration, ?> configuration, Entity player, int value) {
        int previous = this.get(configuration, player);
        int minimum = this.getMinimum(configuration, player);
        int maximum = this.getMaximum(configuration, player);
        value = Mth.clamp(value, minimum, maximum);
        this.set(configuration, player, value);
        Configuration config = configuration.getConfiguration();

        if (previous != value) {
            if (value == minimum) ConfiguredEntityAction.execute(config.minAction(), player);
            if (value == maximum) ConfiguredEntityAction.execute(config.maxAction(), player);
        }
        return value;
    }

    @Override
    public int getMaximum(ConfiguredPower<Configuration, ?> configuration, Entity player) {
        Configuration config = configuration.getConfiguration();
        if (config.staticMax().isPresent()) {
            return config.staticMax().get();
        }
        int level = player instanceof Player p ? LevelingUtils.getPlayerLevel(p) : 1;
        if (config.steppedMax().isPresent()) {
            return config.steppedMax().get().maxForLevel(level);
        }
        LeveledMax lm = config.leveledMax().orElseThrow();
        return (int) (lm.base() + lm.perLevel() * level);
    }

    @SubscribeEvent
    public static void onAptitudeChanged(AptitudeChangedEvent event) {
        Entity entity = event.getPlayer();
        if (!(entity instanceof Player player) || entity.level().isClientSide()) {
            return;
        }
        if (event.getNewLevel() == event.getOldLevel()) {
            return;
        }
        var factory = ModPowers.LEVELED_RESOURCE.get();
        IPowerContainer.get(player).ifPresent(container -> {
            container.getPowers(factory).forEach(powerHolder -> {
                ConfiguredPower<Configuration, ?> configuredPower = powerHolder.value();
                Configuration config = configuredPower.getConfiguration();
                int newMax = factory.getMaximum(configuredPower, player);
                if (config.restoreOnLevelup()) {
                    factory.assign(configuredPower, player, newMax);
                    ApoliAPI.synchronizePowerContainer(player);
                } else if (config.leveledMax().isPresent() || config.steppedMax().isPresent()) {
                    int current = factory.getValue(configuredPower, player);
                    if (current > newMax) {
                        factory.assign(configuredPower, player, current);
                        ApoliAPI.synchronizePowerContainer(player);
                    }
                }
            });
        });
    }

    /**
     * Piecewise max from level "steps": {@code stepCount = floor(level / gap)} (at least 0), optionally capped
     * by {@code maxSteps}, then {@code max = round(start + increment * stepCount)} clamped to int range.
     * <p>
     * {@code increment} applies per completed step (each {@code gap} player levels), not per individual level.
     * With {@code gap = 1}, this is linear in level: {@code round(start + increment * level)}.
     * <p>
     * Example (Wild Magic–style brackets): {@code start=6}, {@code increment=-1}, {@code gap=5},
     * {@code max_steps=3} → max 6,5,4,3 for levels [0–4],[5–9],[10–14],[15+].
     */
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
                Codec.INT.fieldOf("gap").forGetter(SteppedMax::gap),
                CalioCodecHelper.optionalField(CalioCodecHelper.INT, "max_steps").forGetter(SteppedMax::maxSteps)
        ).apply(instance, SteppedMax::new));

        public int maxForLevel(int level) {
            int lvl = Math.max(0, level);
            long stepCount = lvl / (long) gap;
            if (maxSteps.isPresent()) {
                stepCount = Math.min(stepCount, maxSteps.get());
            }
            float raw = start + increment * stepCount;
            return Mth.clamp(Math.round(raw), Integer.MIN_VALUE, Integer.MAX_VALUE);
        }
    }

    public record LeveledMax(float base, float perLevel) {
        public static final Codec<LeveledMax> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.fieldOf("base").forGetter(LeveledMax::base),
                Codec.FLOAT.fieldOf("per_level").forGetter(LeveledMax::perLevel)
        ).apply(instance, LeveledMax::new));
    }

    public record Configuration(
            HudRender hudRender,
            int initialValue,
            int min,
            Optional<Integer> staticMax,
            Optional<LeveledMax> leveledMax,
            Optional<SteppedMax> steppedMax,
            boolean restoreOnLevelup,
            Holder<ConfiguredEntityAction<?, ?>> minAction,
            Holder<ConfiguredEntityAction<?, ?>> maxAction
    ) implements IHudRenderedVariableIntPowerConfiguration {

        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                CalioCodecHelper.optionalField(HudRender.CODEC, "hud_render", HudRender.DONT_RENDER).forGetter(Configuration::hudRender),
                CalioCodecHelper.optionalField(CalioCodecHelper.INT, "start_value").forGetter(c -> c.initialValue() == c.min() ? Optional.empty() : Optional.of(c.initialValue())),
                CalioCodecHelper.INT.fieldOf("min").forGetter(Configuration::min),
                CalioCodecHelper.optionalField(CalioCodecHelper.INT, "max").forGetter(Configuration::staticMax),
                CalioCodecHelper.optionalField(CalioCodecHelper.FLOAT, "max_base").forGetter(c -> c.leveledMax().map(LeveledMax::base)),
                CalioCodecHelper.optionalField(CalioCodecHelper.FLOAT, "max_per_level").forGetter(c -> c.leveledMax().map(LeveledMax::perLevel)),
                CalioCodecHelper.optionalField(SteppedMax.CODEC, "stepped_max").forGetter(Configuration::steppedMax),
                CalioCodecHelper.optionalField(CalioCodecHelper.BOOL, "restore_on_levelup", false).forGetter(Configuration::restoreOnLevelup),
                ConfiguredEntityAction.optional("min_action").forGetter(Configuration::minAction),
                ConfiguredEntityAction.optional("max_action").forGetter(Configuration::maxAction)
        ).apply(instance, (hudRender, startValue, min, maxOpt, maxBaseOpt, maxPerLevelOpt, steppedMaxOpt, restoreOnLevelup, minAction, maxAction) -> {
            int initialValue = startValue.orElse(min);
            Optional<Integer> staticMax = maxOpt;
            boolean hasStatic = staticMax.isPresent();
            boolean hasLinear = maxBaseOpt.isPresent() && maxPerLevelOpt.isPresent();
            boolean hasLinearPartial = maxBaseOpt.isPresent() ^ maxPerLevelOpt.isPresent();
            boolean hasStepped = steppedMaxOpt.isPresent();
            if (hasLinearPartial) {
                throw new IllegalArgumentException("When using linear level-based max, both 'max_base' and 'max_per_level' must be provided");
            }
            int modeCount = (hasStatic ? 1 : 0) + (hasLinear ? 1 : 0) + (hasStepped ? 1 : 0);
            if (modeCount != 1) {
                throw new IllegalArgumentException("Provide exactly one of: 'max', ('max_base' + 'max_per_level'), or 'stepped_max'");
            }
            Optional<LeveledMax> leveledMax = hasLinear ? Optional.of(new LeveledMax(maxBaseOpt.get(), maxPerLevelOpt.get())) : Optional.empty();
            Optional<SteppedMax> steppedMax = hasStepped ? steppedMaxOpt : Optional.empty();
            return new Configuration(hudRender, initialValue, min, staticMax, leveledMax, steppedMax, restoreOnLevelup, minAction, maxAction);
        }));

        @Override
        public boolean isConfigurationValid() {
            return staticMax().isPresent() || leveledMax().isPresent() || steppedMax().isPresent();
        }

        @Override
        public int max() {
            if (staticMax().isPresent()) {
                return staticMax().get();
            }
            return leveledMax().map(lm -> (int) lm.base()).orElseGet(() -> steppedMax().map(sm -> Math.round(sm.start())).orElse(0));
        }
    }
}
