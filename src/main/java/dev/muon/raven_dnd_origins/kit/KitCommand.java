package dev.muon.raven_dnd_origins.kit;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.muon.raven_core.leveling.LevelingUtils;
import dev.muon.raven_dnd_origins.RavenDndOrigins;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public final class KitCommand {
    private static final SuggestionProvider<CommandSourceStack> KITS = (context, builder) -> SharedSuggestionProvider.suggest(DebugKits.names(), builder);

    private KitCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("kit")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("list").executes(context -> list(context.getSource())))
                .then(Commands.literal("apply")
                        .then(Commands.argument("name", StringArgumentType.word()).suggests(KITS)
                                .executes(context -> apply(context.getSource(), StringArgumentType.getString(context, "name"),
                                        List.of(context.getSource().getPlayerOrException())))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(context -> apply(context.getSource(), StringArgumentType.getString(context, "name"),
                                                EntityArgument.getPlayers(context, "targets"))))))
                .then(Commands.literal("save")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(context -> save(context.getSource(), StringArgumentType.getString(context, "name")))));
    }

    private static int list(CommandSourceStack source) {
        List<String> names = DebugKits.names();
        source.sendSuccess(() -> Component.literal(names.isEmpty()
                ? "No kits in " + DebugKits.directory()
                : "Kits: " + String.join(", ", names)), false);
        return names.size();
    }

    private static int apply(CommandSourceStack source, String name, Collection<ServerPlayer> targets) {
        JsonObject kit;
        try {
            kit = DebugKits.read(name);
        } catch (IOException | RuntimeException e) {
            source.sendFailure(Component.literal("Could not read kit " + name + ": " + e.getMessage()));
            return 0;
        }
        int applied = 0;
        for (ServerPlayer target : targets) {
            try {
                report(source, name, target, KitApplier.apply(target, kit));
                applied++;
            } catch (RuntimeException e) {
                RavenDndOrigins.LOGGER.error("Kit {} failed on {}", name, target.getGameProfile().getName(), e);
                source.sendFailure(Component.literal("Kit " + name + " failed on " + target.getGameProfile().getName() + ": " + e.getMessage()));
            }
        }
        return applied;
    }

    private static void report(CommandSourceStack source, String name, ServerPlayer target, List<String> problems) {
        String summary = "Applied " + name + " to " + target.getGameProfile().getName() + " (level " + LevelingUtils.getPlayerLevel(target) + ")";
        source.sendSuccess(() -> Component.literal(problems.isEmpty() ? summary : summary + " with " + problems.size() + " problem(s):"), true);
        for (String problem : problems) {
            RavenDndOrigins.LOGGER.warn("Kit {} on {}: {}", name, target.getGameProfile().getName(), problem);
            source.sendFailure(Component.literal(" - " + problem));
        }
    }

    private static int save(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        try {
            Path file = DebugKits.write(name, KitApplier.capture(player));
            source.sendSuccess(() -> Component.literal("Saved kit " + name + " to " + file), true);
            return 1;
        } catch (IOException e) {
            source.sendFailure(Component.literal("Could not save kit " + name + ": " + e.getMessage()));
            return 0;
        }
    }
}
