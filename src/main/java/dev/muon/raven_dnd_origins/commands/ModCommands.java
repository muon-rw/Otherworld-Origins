package dev.muon.raven_dnd_origins.commands;

import com.mojang.brigadier.CommandDispatcher;
import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.network.DumpClientStateMessage;
import dev.muon.raven_dnd_origins.selection.SelectionSessions;
import dev.muon.raven_dnd_origins.util.OriginStateDumper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.nio.file.Path;
import java.util.List;

@EventBusSubscriber(modid = RavenDndOrigins.MODID)
public class ModCommands {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("raven_dnd_origins")
                .then(Commands.literal("gui")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> openGui(context.getSource(), List.of(context.getSource().getPlayerOrException())))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> openGui(context.getSource(), EntityArgument.getPlayers(context, "targets").stream().toList()))
                        )
                )
                .then(Commands.literal("dumpSpells")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> dumpSpells(context.getSource()))
                )
                .then(Commands.literal("dumpState")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> dumpState(context.getSource(), List.of(context.getSource().getPlayerOrException())))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> dumpState(context.getSource(), EntityArgument.getPlayers(context, "targets").stream().toList()))
                        )
                )
        );
    }

    /**
     * Dumps the origin/power state for each target player on both server and client to the
     * respective {@code logs/} directories. Use when a discrepancy is suspected visually so
     * the two snapshots can be compared. See {@link OriginStateDumper}.
     */
    private static int dumpState(CommandSourceStack source, List<ServerPlayer> targets) {
        String requester = source.getTextName();
        for (ServerPlayer target : targets) {
            String reason = "manual /raven_dnd_origins dumpState (requested by " + requester + ")";
            OriginStateDumper.dump(target, "SERVER", target.getServer(), reason);
            PacketDistributor.sendToPlayer(target, new DumpClientStateMessage(reason));
        }
        if (targets.size() == 1) {
            source.sendSuccess(() -> Component.literal(
                    "Dumped state for " + targets.get(0).getDisplayName().getString() + " (server + client logs/)"), true);
        } else {
            source.sendSuccess(() -> Component.literal(
                    "Dumped state for " + targets.size() + " players (server + client logs/)"), true);
        }
        return targets.size();
    }

    /** Wipes every enabled layer and re-runs full character creation through the session system. */
    private static int openGui(CommandSourceStack source, List<ServerPlayer> targets) {
        for (ServerPlayer target : targets) {
            SelectionSessions.beginFullCreation(target);
        }
        if (targets.size() == 1) {
            source.sendSuccess(() -> Component.literal("Opened origin selection for " + targets.get(0).getDisplayName().getString()), true);
        } else {
            source.sendSuccess(() -> Component.literal("Opened origin selection for " + targets.size() + " players"), true);
        }
        return targets.size();
    }

    private static int dumpSpells(CommandSourceStack source) {
        int count = SpellDump.write(source.getServer());
        if (count > 0) {
            Path outputFile = FMLPaths.CONFIGDIR.get().resolve(RavenDndOrigins.MODID).resolve("spells.json");
            source.sendSuccess(() -> Component.literal("Spell dump complete! Written to: " + outputFile), true);
        } else {
            source.sendFailure(Component.literal("Failed to write spell dump; see server log"));
        }
        return count;
    }
}
