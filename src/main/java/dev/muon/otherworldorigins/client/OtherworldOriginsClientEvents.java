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

import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = OtherworldOrigins.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class OtherworldOriginsClientEvents {

    private static final Set<ResourceLocation> DYNAMIC_LAYER_IDS = Set.of(
            OtherworldOrigins.loc("first_feat"), OtherworldOrigins.loc("second_feat"),
            OtherworldOrigins.loc("third_feat"), OtherworldOrigins.loc("fourth_feat"),
            OtherworldOrigins.loc("fifth_feat"),
            OtherworldOrigins.loc("plus_one_aptitude_resilient"), OtherworldOrigins.loc("wildshape"),
            OtherworldOrigins.loc("chemical_mastery"),
            OtherworldOrigins.loc("magical_secrets")
    );

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        Minecraft instance = Minecraft.getInstance();
        if (OriginsClient.AWAITING_DISPLAY.get() && instance.screen == null && instance.player != null) {
            IOriginContainer.get(instance.player).ifPresent(container -> {
                List<Holder.Reference<OriginLayer>> missingLayers = OriginsAPI.getActiveLayers().stream()
                        .filter(x -> !container.hasOrigin(x))
                        .sorted(Comparator.comparing(Holder::value))
                        .toList();
                if (!missingLayers.isEmpty()) {
                    boolean allMissingAreDynamic = missingLayers.stream()
                            .allMatch(l -> DYNAMIC_LAYER_IDS.contains(l.key().location()));

                    if (allMissingAreDynamic) {
                        OtherworldOrigins.LOGGER.debug("[OWClientEvents] AWAITING_DISPLAY: dynamic prompt with {} layers", missingLayers.size());
                        instance.setScreen(new OtherworldOriginScreen(ImmutableList.copyOf(missingLayers), 0, OriginsClient.SHOW_DIRT_BACKGROUND, true));
                    } else {
                        List<Holder.Reference<OriginLayer>> allLayers = OriginsAPI.getActiveLayers().stream()
                                .sorted(Comparator.comparing(Holder::value))
                                .toList();
                        OtherworldOrigins.LOGGER.debug("[OWClientEvents] AWAITING_DISPLAY: full selection with {} layers", allLayers.size());
                        instance.setScreen(new OtherworldOriginScreen(ImmutableList.copyOf(allLayers), 0, OriginsClient.SHOW_DIRT_BACKGROUND, false));
                    }
                    OriginsClient.AWAITING_DISPLAY.set(false);
                }
            });
        }
    }
}
