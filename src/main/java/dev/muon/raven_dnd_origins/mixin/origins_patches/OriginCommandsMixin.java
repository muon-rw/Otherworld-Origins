package dev.muon.raven_dnd_origins.mixin.origins_patches;

import com.mojang.brigadier.context.CommandContext;
import dev.muon.raven_dnd_origins.selection.SelectionSessions;
import dev.muon.raven_dnd_origins.selection.SessionKind;
import dev.overgrown.origins.command.OriginCommands;
import dev.overgrown.origins.origin.OriginLayer;
import dev.overgrown.origins.origin.OriginLayers;
import dev.overgrown.origins.origin.OriginManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.List;

/**
 * Routes {@code /origin gui} through {@link SelectionSessions}. Overgrown's executor only pushes its
 * single-layer choose screen with no persisted state, which the client swaps for a session-less copy
 * of ours: nothing survives a relog and no confirm screen follows. Upstream Origins wiped every
 * layer here, so the no-layer form is full character creation, like {@code /raven_dnd_origins gui};
 * with a layer it is a scoped re-pick of that layer.
 */
@Mixin(value = OriginCommands.class, remap = false)
public abstract class OriginCommandsMixin {

    @Inject(method = "gui", at = @At("HEAD"), cancellable = true)
    private static void raven_dnd_origins$routeGuiThroughSession(CommandContext<CommandSourceStack> ctx,
                                                                 Collection<ServerPlayer> targets,
                                                                 ResourceLocation layerId,
                                                                 CallbackInfoReturnable<Integer> cir) {
        CommandSourceStack source = ctx.getSource();
        OriginLayer layer = layerId == null ? null : OriginLayers.get(layerId);
        if (layerId != null && layer == null) {
            source.sendFailure(Component.literal("Unknown origin layer " + layerId));
            cir.setReturnValue(0);
            return;
        }
        int opened = 0;
        for (ServerPlayer player : targets) {
            if (layer == null) {
                SelectionSessions.beginFullCreation(player);
                opened++;
                continue;
            }
            // A layer the player cannot currently pick from would be cleared and then completed
            // immediately, leaving it empty behind a confirm screen.
            if (!OriginLayers.enabledFor(player).contains(layer) || !OriginManager.hasChoosableOrigins(player, layer)) {
                source.sendFailure(Component.literal(player.getGameProfile().getName() + " has no choosable origins on " + layerId)
                        .withStyle(ChatFormatting.RED));
                continue;
            }
            SelectionSessions.beginCleared(player, List.of(layerId), SessionKind.RESELECTION);
            opened++;
        }
        int finalOpened = opened;
        source.sendSuccess(() -> Component.literal("Opened origin selection for " + finalOpened + " player(s)"), true);
        cir.setReturnValue(opened);
    }
}
