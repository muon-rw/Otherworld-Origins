package dev.muon.raven_dnd_origins.power;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.muon.raven_core.leveling.event.AptitudeChangedEvent;
import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.selection.SelectionSessions;
import dev.overgrown.apoli.action.EntityAction;
import dev.overgrown.apoli.condition.context.EntityCtx;
import dev.overgrown.apoli.power.PowerLookup;
import dev.overgrown.apoli.power.PowerType;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = RavenDndOrigins.MODID)
public class ActionOnLevelupPower extends PowerType<ActionOnLevelupPower.Configuration> {

    private static final MapCodec<Configuration> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            EntityAction.CODEC.fieldOf("entity_action").forGetter(Configuration::entityAction)
    ).apply(instance, Configuration::new));

    @Override
    public MapCodec<Configuration> configCodec() {
        return CODEC;
    }

    public static void execute(ServerPlayer player) {
        PowerLookup.forEach(player, ModPowers.ACTION_ON_LEVELUP, Configuration.class,
                cfg -> cfg.entityAction().run(new EntityCtx(player, player.level())));
    }

    @SubscribeEvent
    public static void onAptitudeChanged(AptitudeChangedEvent event) {
        if (event.getNewLevel() <= event.getOldLevel()) {
            return;
        }
        if (event.getPlayer() instanceof ServerPlayer serverPlayer) {
            execute(serverPlayer);
            SelectionSessions.promptLevelGated(serverPlayer);
        }
    }

    public record Configuration(EntityAction entityAction) {}
}
