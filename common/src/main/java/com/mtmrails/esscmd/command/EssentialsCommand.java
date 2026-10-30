package com.mtmrails.esscmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mtmrails.esscmd.CmdEssentials;
import com.mtmrails.esscmd.config.ConfigManager;
import com.mtmrails.esscmd.permission.PermissionNodes;
import com.mtmrails.esscmd.platform.Platform;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/** {@code /cmdessentials [reload]}, alias {@code /ess}. Replaces Essentials' {@code /essentials}. */
public final class EssentialsCommand {
    private EssentialsCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> root = dispatcher.register(Commands.literal("cmdessentials")
                .requires(PermissionNodes.ESSENTIALS.require())
                .executes(ctx -> CommandUtil.ok(ctx.getSource(), "version",
                        Platform.get().modVersion(), Platform.get().loaderName()))
                .then(Commands.literal("reload")
                        .requires(PermissionNodes.ESSENTIALS_RELOAD.require())
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            ConfigManager.reload(source.getServer()).whenCompleteAsync((ignored, error) -> {
                                if (error == null) {
                                    CommandUtil.ok(source, "reload.done");
                                } else {
                                    CmdEssentials.LOGGER.error("cmdEssentials reload failed", error);
                                    Throwable cause = error.getCause() != null ? error.getCause() : error;
                                    CommandUtil.fail(source, "reload.failed", cause.getMessage());
                                }
                            }, source.getServer());
                            return 1;
                        })));
        // A bare redirect doesn't run the target's executes(), so copy it for plain `/ess`.
        dispatcher.register(Commands.literal("ess")
                .requires(root.getRequirement())
                .executes(root.getCommand())
                .redirect(root));
    }
}
