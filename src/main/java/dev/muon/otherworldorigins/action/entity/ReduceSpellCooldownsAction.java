package dev.muon.otherworldorigins.action.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.otherworldorigins.util.spell.SpellCastUtil;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.EntityAction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Apoli entity action: reduces remaining Iron's Spellbooks cooldown on every spell currently on cooldown
 * by {@code ticks} game ticks. Same sync path as {@link ResetSpellCooldownsAction}.
 */
public class ReduceSpellCooldownsAction extends EntityAction<ReduceSpellCooldownsAction.Configuration> {

    public ReduceSpellCooldownsAction() {
        super(Configuration.CODEC);
    }

    @Override
    public void execute(Configuration configuration, Entity entity) {
        if (!(entity instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        SpellCastUtil.reducePlayerSpellCooldowns(player, configuration.ticks());
    }

    public record Configuration(int ticks) implements IDynamicFeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("ticks").forGetter(Configuration::ticks)
        ).apply(instance, Configuration::new));

        @Override
        public boolean isConfigurationValid() {
            return true;
        }
    }
}
