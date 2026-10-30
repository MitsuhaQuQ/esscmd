package com.mtmrails.esscmd.command;

import java.util.Map;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mtmrails.esscmd.CmdEssentials;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * Short aliases that redirect to vanilla commands, e.g. {@code /gm creative} for
 * {@code /gamemode creative}. The vanilla command keeps doing the work, including its own
 * permission check. Must run after vanilla registered its commands, which both loaders'
 * command events guarantee.
 */
public final class VanillaAliases {
    /** alias -> vanilla command */
    private static final Map<String, String> ALIASES = Map.of("gm", "gamemode");

    private VanillaAliases() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        ALIASES.forEach((alias, target) -> {
            CommandNode<CommandSourceStack> node = dispatcher.getRoot().getChild(target);
            if (node == null) {
                CmdEssentials.LOGGER.warn("Vanilla command /{} not found, skipping alias /{}", target, alias);
                return;
            }
            dispatcher.register(Commands.literal(alias)
                    .requires(node.getRequirement())
                    .executes(node.getCommand())
                    .redirect(node));
        });
    }
}
