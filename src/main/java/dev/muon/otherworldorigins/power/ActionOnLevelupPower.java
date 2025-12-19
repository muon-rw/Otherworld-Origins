package dev.muon.otherworldorigins.power;

import dev.muon.otherworld.leveling.event.AptitudeChangedEvent;
import dev.muon.otherworldorigins.OtherworldOrigins;
import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.apoli.data.ApoliDataTypes;
import io.github.apace100.apoli.power.Power;
import io.github.apace100.apoli.power.PowerType;
import io.github.apace100.apoli.power.factory.PowerFactory;
import io.github.apace100.calio.data.SerializableData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.function.Consumer;

@Mod.EventBusSubscriber(modid = "otherworldorigins")
public class ActionOnLevelupPower extends Power {
    private final Consumer<Entity> entityAction;

    public ActionOnLevelupPower(PowerType<?> type, LivingEntity entity, Consumer<Entity> entityAction) {
        super(type, entity);
        this.entityAction = entityAction;
    }

    public void executeAction(Entity entity) {
        if (entityAction != null) {
            entityAction.accept(entity);
        }
    }

    @SubscribeEvent
    public static void onAptitudeChanged(AptitudeChangedEvent event) {
        if (event.getNewLevel() > event.getOldLevel()) {
            Entity entity = event.getPlayer();
            if (entity instanceof ServerPlayer serverPlayer) {
                PowerHolderComponent.getPowers(serverPlayer, ActionOnLevelupPower.class).forEach(power -> {
                    power.executeAction(serverPlayer);
                });
            }
        }
    }

    public static PowerFactory<?> createFactory() {
        return new PowerFactory<>(
                OtherworldOrigins.loc("action_on_levelup"),
                new SerializableData()
                        .add("entity_action", ApoliDataTypes.ENTITY_ACTION, null),
                data -> (type, entity) -> new ActionOnLevelupPower(
                        type,
                        entity,
                        data.get("entity_action")
                )
        ).allowCondition();
    }
}
