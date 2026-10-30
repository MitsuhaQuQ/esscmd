package com.mtmrails.esscmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mtmrails.esscmd.permission.PermissionNodes;
import com.mtmrails.esscmd.player.AfkService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /afk [player/message...]}: toggles AFK. Like Essentials, a lone argument that names
 * an online player toggles that player when the source has afk.others; otherwise it's the
 * AFK message.
 */
public final class AfkCommand {
    private AfkCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("afk")
                .requires(PermissionNodes.AFK.require())
                .executes(ctx -> toggle(ctx.getSource().getPlayerOrException(), null))
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            String arg = StringArgumentType.getString(ctx, "message");
                            ServerPlayer named = arg.contains(" ") ? null
                                    : source.getServer().getPlayerList().getPlayerByName(arg);
                            if (named != null && PermissionNodes.AFK_OTHERS.check(source)) {
                                return toggle(named, null);
                            }
                            return toggle(source.getPlayerOrException(), arg);
                        })));
    }

    private static int toggle(ServerPlayer player, String message) {
        AfkService.setAfk(player, !AfkService.isAfk(player), message);
        return 1;
    }
}
