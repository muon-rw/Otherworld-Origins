package dev.muon.otherworldorigins.util;

import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.power.ShapeshiftPower;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredEntityCondition;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = OtherworldOrigins.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ShapeshiftSpellHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onSpellPreCast(SpellPreCastEvent event) {
        Player player = event.getEntity();
        if (player == null) return;

        ShapeshiftPower.Configuration config = ShapeshiftPower.getActiveShapeshiftConfig(player);
        if (config == null) return;

        if (ConfiguredEntityCondition.check(config.preventSpellCasts(), player)) {
            event.setCanceled(true);
            player.displayClientMessage(
                    Component.translatable("message.otherworldorigins.shapeshift_spell_blocked")
                            .withStyle(ChatFormatting.RED),
                    true
            );
        }
    }
}
