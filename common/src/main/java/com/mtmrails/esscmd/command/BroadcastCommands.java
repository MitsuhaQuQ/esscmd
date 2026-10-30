package com.mtmrails.esscmd.command;

import java.util.List;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.CommandNode;
import com.mtmrails.esscmd.compat.Players;
import com.mtmrails.esscmd.message.Messages;
import com.mtmrails.esscmd.permission.PermissionNodes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Server-wide and dimension-wide broadcasts. */
public final class BroadcastCommands {
    private BroadcastCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandNode<CommandSourceStack> broadcast = dispatcher.register(Commands.literal("broadcast")
                .requires(PermissionNodes.BROADCAST.require())
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> broadcast(ctx.getSource(), StringArgumentType.getString(ctx, "message")))));
        for (String alias : List.of("bc", "bcast", "shout")) {
            dispatcher.register(Commands.literal(alias).requires(PermissionNodes.BROADCAST.require()).redirect(broadcast));
        }

        CommandNode<CommandSourceStack> world = dispatcher.register(Commands.literal("broadcastworld")
                .requires(PermissionNodes.BROADCASTWORLD.require())
                .then(Commands.argument("world", DimensionArgument.dimension())
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(ctx -> broadcastWorld(ctx.getSource(), DimensionArgument.getDimension(ctx, "world"),
                                        StringArgumentType.getString(ctx, "message"))))));
        for (String alias : List.of("bcw", "bcastw", "shoutworld")) {
            dispatcher.register(Commands.literal(alias).requires(PermissionNodes.BROADCASTWORLD.require()).redirect(world));
        }
    }

    private static int broadcast(CommandSourceStack source, String text) {
        int count = 0;
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            Players.send(player, Messages.info(player, "broadcast.message", text));
            count++;
        }
        Players.sendToConsole(source.getServer(), Messages.info(source, "broadcast.message", text));
        return count;
    }

    private static int broadcastWorld(CommandSourceStack source, ServerLevel level, String text) {
        int count = 0;
        for (ServerPlayer player : level.players()) {
            Players.send(player, Messages.info(player, "broadcastworld.message", text));
            count++;
        }
        Players.sendToConsole(source.getServer(), Messages.info(source, "broadcastworld.console",
                level.dimension().location(), text));
        return count;
    }
}
