package com.mtmrails.esscmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mtmrails.esscmd.compat.Texts;
import com.mtmrails.esscmd.permission.PermissionNodes;
import com.mtmrails.esscmd.platform.Platform;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/** {@code /cmdessentials [reload]}, alias {@code /ess}. Replaces Essentials' {@code /essentials}. */
public final class EssentialsCommand {
    private EssentialsCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> root = dispatcher.register(Commands.literal("cmdessentials")
                .requires(PermissionNodes.ESSENTIALS.require())
                .executes(ctx -> {
                    Platform platform = Platform.get();
                    Texts.success(ctx.getSource(), Texts.literal("cmdEssentials " + platform.modVersion()
                            + " (" + platform.loaderName() + ")").withStyle(ChatFormatting.GOLD), false);
                    return 1;
                })
                .then(Commands.literal("reload")
                        .requires(PermissionNodes.ESSENTIALS_RELOAD.require())
                        .executes(ctx -> {
                            // Nothing is configurable yet; config and message files hook in here.
                            Texts.success(ctx.getSource(), Texts.literal("cmdEssentials reloaded.")
                                    .withStyle(ChatFormatting.GOLD), true);
                            return 1;
                        })));
        // A bare redirect doesn't run the target's executes(), so copy it for plain `/ess`.
        dispatcher.register(Commands.literal("ess")
                .requires(root.getRequirement())
                .executes(root.getCommand())
                .redirect(root));
    }
}
