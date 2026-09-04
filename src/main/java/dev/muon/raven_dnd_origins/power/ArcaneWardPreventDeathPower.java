package dev.muon.raven_dnd_origins.power;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.util.spell.SpellCastUtil;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.codec.LoggedOptionalField;
import dev.overgrown.apoli.condition.DamageCondition;
import dev.overgrown.apoli.condition.context.DamageCtx;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * When lethal damage would kill a {@link ServerPlayer}, they survive at half a heart if they have enough mana
 * to pay {@code lethalDamage * mana_multiplier} (default 4). If current mana is insufficient to stay above 0
 * after paying, death is not prevented.
 * <p>
 * Runs at {@link EventPriority#HIGHEST}, as on 1.20.1: Hardcore Revival's knockout listener is registered at
 * {@link EventPriority#HIGH} and cancels the death, and the bus skips later listeners once an event is cancelled.
 */
@EventBusSubscriber(modid = RavenDndOrigins.MODID)
public class ArcaneWardPreventDeathPower extends PowerType<ArcaneWardPreventDeathPower.Configuration> {

    private static final Map<ServerPlayer, Float> LAST_DAMAGE = Collections.synchronizedMap(new WeakHashMap<>());

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LoggedOptionalField.of("entity_action", EntityAction.CODEC).forGetter(Configuration::entityAction),
            LoggedOptionalField.strict("damage_condition", DamageCondition.CODEC).forGetter(Configuration::damageCondition),
            Codec.FLOAT.optionalFieldOf("mana_multiplier", 4.0F).forGetter(Configuration::manaMultiplier)
    ).apply(instance, Configuration::new));

    @Override
    public MapCodec<Configuration> configCodec() {
        return CODEC;
    }

    @SubscribeEvent
    public static void recordLethalDamage(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LAST_DAMAGE.put(player, event.getNewDamage());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void preventDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Float amount = LAST_DAMAGE.remove(player);
        if (amount != null && tryPreventDeath(player, event.getSource(), amount)) {
            event.setCanceled(true);
        }
    }

    public static boolean tryPreventDeath(Entity entity, DamageSource source, float lethalDamageAmount) {
        if (lethalDamageAmount <= 0.0F || !(entity instanceof ServerPlayer player) || player.level().isClientSide()) {
            return false;
        }
        DamageCtx damageCtx = new DamageCtx(source, player, player.level(), lethalDamageAmount);
        for (Configuration cfg : PowerLookup.active(player, ModPowers.ARCANE_WARD_PREVENT_DEATH, Configuration.class)) {
            if (cfg.damageCondition().isPresent() && !cfg.damageCondition().get().test(damageCtx)) {
                continue;
            }
            return applyWard(player, cfg, lethalDamageAmount);
        }
        return false;
    }

    private static boolean applyWard(ServerPlayer player, Configuration cfg, float lethalDamageAmount) {
        float drain = lethalDamageAmount * cfg.manaMultiplier();
        if (drain <= 0.0F) {
            return false;
        }

        if (player.getAbilities().instabuild) {
            survive(player, cfg);
            return true;
        }

        MagicData magicData = MagicData.getPlayerMagicData(player);
        if (magicData.getMana() <= drain) {
            return false;
        }

        SpellCastUtil.drainPlayerMana(player, drain);
        survive(player, cfg);
        return true;
    }

    private static void survive(ServerPlayer player, Configuration cfg) {
        player.setHealth(1.0F);
        cfg.entityAction().ifPresent(action -> action.run(new EntityCtx(player, player.level())));
    }

    public record Configuration(
            Optional<EntityAction> entityAction,
            Optional<DamageCondition> damageCondition,
            float manaMultiplier
    ) {}
}
