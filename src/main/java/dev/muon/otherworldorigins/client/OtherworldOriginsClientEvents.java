package dev.muon.otherworldorigins.client;

import com.google.common.collect.ImmutableList;
import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.client.screen.OtherworldOriginScreen;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import io.github.edwinmindcraft.origins.client.OriginsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.List;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = OtherworldOrigins.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class OtherworldOriginsClientEvents {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        Minecraft instance = Minecraft.getInstance();
        if (OriginsClient.AWAITING_DISPLAY.get() && instance.screen == null && instance.player != null) {
            IOriginContainer.get(instance.player).ifPresent(container -> {
                List<Holder.Reference<OriginLayer>> layers = OriginsAPI.getActiveLayers().stream()
                        .filter(x -> !container.hasOrigin(x))
                        .sorted(Comparator.comparing(Holder::value))
                        .toList();
                if (!layers.isEmpty()) {
                    instance.setScreen(new OtherworldOriginScreen(ImmutableList.copyOf(layers), 0, OriginsClient.SHOW_DIRT_BACKGROUND));
                    OriginsClient.AWAITING_DISPLAY.set(false);
                }
            });
        }
    }
}
