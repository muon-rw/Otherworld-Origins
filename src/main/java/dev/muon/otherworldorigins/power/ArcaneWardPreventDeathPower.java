package dev.muon.otherworldorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.otherworldorigins.util.spell.SpellCastUtil;
import io.github.edwinmindcraft.apoli.api.IDynamicFeatureConfiguration;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredDamageCondition;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredEntityAction;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

/**
 * When lethal damage would kill a {@link ServerPlayer}, they survive at half a heart if they have enough mana
 * to pay {@code lethalDamage * mana_multiplier} (default 4). If current mana is insufficient to stay above 0
 * after paying, death is not prevented.
 */
public class ArcaneWardPreventDeathPower extends PowerFactory<ArcaneWardPreventDeathPower.Configuration> {

    public ArcaneWardPreventDeathPower() {
        super(Configuration.CODEC);
    }

    public static boolean tryPreventDeath(Entity entity, DamageSource source, float lethalDamageAmount) {
        if (lethalDamageAmount <= 0.0F || !(entity instanceof ServerPlayer player) || player.level().isClientSide()) {
            return false;
        }

        var first = IPowerContainer.getPowers(entity, ModPowers.ARCANE_WARD_PREVENT_DEATH.get()).stream()
                .filter(holder -> {
                    Configuration cfg = holder.value().getConfiguration();
                    return ConfiguredDamageCondition.check(cfg.damageCondition(), source, lethalDamageAmount);
                })
                .map(Holder::value)
                .findFirst();

        return first.map(power -> applyWard(player, power.getConfiguration(), lethalDamageAmount)).orElse(false);
    }

    private static boolean applyWard(ServerPlayer player, Configuration cfg, float lethalDamageAmount) {
        float drain = lethalDamageAmount * cfg.manaMultiplier();
        if (drain <= 0.0F) {
            return false;
        }

        if (player.getAbilities().instabuild) {
            player.setHealth(1.0F);
            ConfiguredEntityAction.execute(cfg.entityAction(), player);
            return true;
        }

        MagicData magicData = MagicData.getPlayerMagicData(player);
        float mana = magicData.getMana();
        if (mana <= drain) {
            return false;
        }

        player.setHealth(1.0F);
        SpellCastUtil.drainPlayerMana(player, drain);
        ConfiguredEntityAction.execute(cfg.entityAction(), player);
        return true;
    }

    public record Configuration(
            Holder<ConfiguredEntityAction<?, ?>> entityAction,
            Holder<ConfiguredDamageCondition<?, ?>> damageCondition,
            float manaMultiplier
    ) implements IDynamicFeatureConfiguration {

        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ConfiguredEntityAction.optional("entity_action").forGetter(Configuration::entityAction),
                ConfiguredDamageCondition.optional("damage_condition").forGetter(Configuration::damageCondition),
                Codec.FLOAT.optionalFieldOf("mana_multiplier", 4.0F).forGetter(Configuration::manaMultiplier)
        ).apply(instance, Configuration::new));
    }
}
