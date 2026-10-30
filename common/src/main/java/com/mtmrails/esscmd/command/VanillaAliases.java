package com.mtmrails.esscmd.command;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.mtmrails.esscmd.CmdEssentials;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

/**
 * Shortcuts for vanilla commands. They never replace vanilla behaviour: each one runs the real
 * vanilla command through the dispatcher, so its permission check, feedback and logging stay.
 * Must run after vanilla registered its commands, which both loaders' command events guarantee.
 *
 * <ul>
 *   <li>{@code /gm} redirects to {@code /gamemode}.</li>
 *   <li>{@code /gamemode} (and so {@code /gm}) also accepts {@code c s a sp} and {@code 0-3}.</li>
 *   <li>{@code /gmc /gms /gma /gmsp [targets]} set creative, survival, adventure, spectator.</li>
 * </ul>
 */
public final class VanillaAliases {
    private static final String GAMEMODE = "gamemode";

    /** A gamemode with the short names Essentials accepts for it. */
    private record Mode(String name, String command, List<String> shortNames) {}

    private static final List<Mode> MODES = List.of(
            new Mode("survival", "gms", List.of("s", "0")),
            new Mode("creative", "gmc", List.of("c", "1")),
            new Mode("adventure", "gma", List.of("a", "2")),
            new Mode("spectator", "gmsp", List.of("sp", "3")));

    private static final Map<String, String> ALIASES = Map.of("gm", GAMEMODE);

    private VanillaAliases() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandNode<CommandSourceStack> gamemode = dispatcher.getRoot().getChild(GAMEMODE);
        if (gamemode == null) {
            CmdEssentials.LOGGER.warn("Vanilla /gamemode not found, skipping gamemode shortcuts");
            return;
        }
        Predicate<CommandSourceStack> allowed = gamemode.getRequirement();

        // Short mode names under /gamemode. Registering a literal that already exists merges
        // our children into vanilla's node and keeps its command and requirement.
        LiteralArgumentBuilder<CommandSourceStack> shortNames = Commands.literal(GAMEMODE);
        for (Mode mode : MODES) {
            for (String shortName : mode.shortNames()) {
                shortNames.then(withTargets(Commands.literal(shortName).requires(allowed), mode, dispatcher));
            }
        }
        dispatcher.register(shortNames);

        for (Mode mode : MODES) {
            dispatcher.register(withTargets(Commands.literal(mode.command()).requires(allowed), mode, dispatcher));
        }

        ALIASES.forEach((alias, target) -> {
            CommandNode<CommandSourceStack> node = dispatcher.getRoot().getChild(target);
            dispatcher.register(Commands.literal(alias)
                    .requires(node.getRequirement())
                    .executes(node.getCommand())
                    .redirect(node));
        });
    }

    /** {@code <literal> [targets]}, running {@code /gamemode <mode> [target]} for each target. */
    private static LiteralArgumentBuilder<CommandSourceStack> withTargets(LiteralArgumentBuilder<CommandSourceStack> literal,
                                                                           Mode mode, CommandDispatcher<CommandSourceStack> dispatcher) {
        return literal
                .executes(ctx -> dispatcher.execute(GAMEMODE + " " + mode.name(), ctx.getSource()))
                .then(Commands.argument("targets", EntityArgument.players())
                        .executes(ctx -> setFor(dispatcher, ctx.getSource(), mode, EntityArgument.getPlayers(ctx, "targets"))));
    }

    private static int setFor(CommandDispatcher<CommandSourceStack> dispatcher, CommandSourceStack source, Mode mode,
                              Collection<ServerPlayer> targets) throws CommandSyntaxException {
        int changed = 0;
        for (ServerPlayer target : targets) {
            // The UUID is a valid entity selector and is unambiguous.
            changed += dispatcher.execute(GAMEMODE + " " + mode.name() + " " + target.getStringUUID(), source);
        }
        return changed;
    }
}
