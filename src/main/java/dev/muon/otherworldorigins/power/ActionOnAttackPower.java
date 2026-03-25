package dev.muon.otherworldorigins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.apace100.apoli.util.HudRender;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.configuration.MustBeBound;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredBiEntityAction;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredBiEntityCondition;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.apoli.api.power.configuration.power.ICooldownPowerConfiguration;
import io.github.edwinmindcraft.apoli.api.power.factory.power.CooldownPowerFactory;
import io.github.edwinmindcraft.calio.api.network.CalioCodecHelper;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Fires a bi-entity action when the power holder melee-attacks an entity.
 * Unlike {@code origins:action_on_hit}, this hooks into {@link AttackEntityEvent}
 * which only fires from {@link Player#attack} — spell and projectile damage
 * will never trigger it.
 */
@Mod.EventBusSubscriber(modid = "otherworldorigins")
public class ActionOnAttackPower extends CooldownPowerFactory.Simple<ActionOnAttackPower.Configuration> {

    public ActionOnAttackPower() {
        super(Configuration.CODEC);
    }

    private void onAttack(ConfiguredPower<Configuration, ?> power, Entity attacker, Entity target) {
        if (this.canUse(power, attacker)
                && ConfiguredBiEntityCondition.check(power.getConfiguration().biEntityCondition(), attacker, target)) {
            ConfiguredBiEntityAction.execute(power.getConfiguration().biEntityAction(), attacker, target);
            this.use(power, attacker);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        Entity target = event.getTarget();
        IPowerContainer.getPowers(player, ModPowers.ACTION_ON_ATTACK.get())
                .forEach(powerHolder -> {
                    ActionOnAttackPower factory = (ActionOnAttackPower) powerHolder.value().getFactory();
                    factory.onAttack(powerHolder.value(), player, target);
                });
    }

    public record Configuration(
            int duration,
            HudRender hudRender,
            Holder<ConfiguredBiEntityCondition<?, ?>> biEntityCondition,
            @MustBeBound Holder<ConfiguredBiEntityAction<?, ?>> biEntityAction
    ) implements ICooldownPowerConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                CalioCodecHelper.optionalField(CalioCodecHelper.INT, "cooldown", 1).forGetter(Configuration::duration),
                CalioCodecHelper.optionalField(HudRender.CODEC, "hud_render", HudRender.DONT_RENDER).forGetter(Configuration::hudRender),
                ConfiguredBiEntityCondition.optional("bientity_condition").forGetter(Configuration::biEntityCondition),
                ConfiguredBiEntityAction.required("bientity_action").forGetter(Configuration::biEntityAction)
        ).apply(instance, Configuration::new));
    }
}
