package dev.muon.otherworldorigins.power.factory;

import dev.muon.otherworldorigins.power.ModPowers;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.configuration.HolderConfiguration;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredEntityAction;
import io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory;
import io.github.edwinmindcraft.apoli.common.component.PowerContainer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import dev.muon.medieval.leveling.event.AptitudeChangedEvent;


@Mod.EventBusSubscriber(modid = "otherworldorigins")
public class ActionOnLevelupPowerFactory extends PowerFactory<HolderConfiguration<ConfiguredEntityAction<?, ?>>> {

    public ActionOnLevelupPowerFactory() {
        super(HolderConfiguration.required(ConfiguredEntityAction.required("entity_action")));
    }

    public static void execute(PowerContainer container, ServerPlayer player, int level) {
        if (level > 0) {
            var powers = container.getPowers(ModPowers.ACTION_ON_LEVELUP.get());
            powers.forEach(powerHolder -> {
                ConfiguredEntityAction.execute(powerHolder.value().getConfiguration().holder(), player);
            });
        }
    }

    @SubscribeEvent
    public static void onAptitudeChanged(AptitudeChangedEvent event) {
        if (event.getNewLevel() > event.getOldLevel()) {
            Entity entity = event.getPlayer();
            if (entity instanceof ServerPlayer serverPlayer) {
                IPowerContainer.get(serverPlayer).ifPresent(container ->
                        execute((PowerContainer) container, serverPlayer, event.getNewLevel())
                );
            }
        }
    }
}