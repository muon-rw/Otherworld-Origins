package dev.muon.raven_dnd_origins.mixin.origins_patches;

import com.mojang.brigadier.context.CommandContext;
import dev.muon.raven_dnd_origins.selection.SelectionSessions;
import dev.overgrown.origins.command.OriginCommands;
import dev.overgrown.origins.origin.OriginLayers;
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

/**
 * Routes {@code /origin gui} through {@link SelectionSessions}. Overgrown's executor only pushes its
 * single-layer choose screen with no persisted state, which the client swaps for a session-less copy
 * of ours: nothing survives a relog and no confirm screen follows. The {@code reset} flag is what
 * Origins 1.28+ passes for the plain and targeted forms (wipe, then reopen); {@code /origin gui
 * unchosen} passes false and only prompts for layers still empty.
 */
@Mixin(value = OriginCommands.class, remap = false)
public abstract class OriginCommandsMixin {

    @Inject(method = "gui", at = @At("HEAD"), cancellable = true)
    private static void raven_dnd_origins$routeGuiThroughSession(CommandContext<CommandSourceStack> ctx,
                                                                 Collection<ServerPlayer> targets,
                                                                 ResourceLocation layerId,
                                                                 boolean reset,
                                                                 CallbackInfoReturnable<Integer> cir) {
        CommandSourceStack source = ctx.getSource();
        if (layerId != null && OriginLayers.get(layerId) == null) {
            source.sendFailure(Component.literal("Unknown origin layer " + layerId));
            cir.setReturnValue(0);
            return;
        }
        int opened = 0;
        for (ServerPlayer player : targets) {
            boolean started = reset
                    ? SelectionSessions.beginFullCreation(player)
                    : SelectionSessions.promptUnchosen(player);
            if (started) {
                opened++;
            }
        }
        if (opened == 0) {
            source.sendFailure(Component.literal(reset
                    ? "No origin layer is enabled for those players."
                    : "Those players have already chosen an origin on every layer.").withStyle(ChatFormatting.RED));
            cir.setReturnValue(0);
            return;
        }
        int finalOpened = opened;
        source.sendSuccess(() -> Component.literal("Opened origin selection for " + finalOpened + " player(s)"), true);
        cir.setReturnValue(opened);
    }
}
