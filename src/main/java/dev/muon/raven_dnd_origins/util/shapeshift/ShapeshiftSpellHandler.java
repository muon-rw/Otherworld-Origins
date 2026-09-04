package dev.muon.raven_dnd_origins.util.shapeshift;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.power.ShapeshiftPower;
import dev.overgrown.apoli.condition.EntityCondition;
import dev.overgrown.apoli.condition.context.EntityCtx;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Optional;

@EventBusSubscriber(modid = RavenDndOrigins.MODID)
public class ShapeshiftSpellHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onSpellPreCast(SpellPreCastEvent event) {
        if (event.getCastSource() == CastSource.COMMAND) return;

        Player player = event.getEntity();
        if (player == null) return;

        ShapeshiftPower.Configuration config = ShapeshiftPower.getActiveShapeshiftConfig(player);
        if (config == null) return;

        Optional<EntityCondition> condition = config.preventSpellCasts();
        if (condition.isPresent() && condition.get().test(EntityCtx.of(player, player.level()))) {
            event.setCanceled(true);
            player.displayClientMessage(
                    Component.translatable("message.raven_dnd_origins.shapeshift_spell_blocked")
                            .withStyle(ChatFormatting.RED),
                    true
            );
        }
    }
}
