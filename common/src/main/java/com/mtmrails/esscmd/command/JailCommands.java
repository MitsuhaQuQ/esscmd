package com.mtmrails.esscmd.command;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mtmrails.esscmd.data.EssData;
import com.mtmrails.esscmd.data.StoredLocation;
import com.mtmrails.esscmd.permission.PermissionNodes;
import com.mtmrails.esscmd.player.JailService;
import com.mtmrails.esscmd.util.DateDiff;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /setjail}, {@code /deljail}, {@code /jails}, {@code /jailedplayers},
 * {@code /togglejail <player> [jail] [datediff]} (aliases {@code /jail}, {@code /unjail}).
 * Jailing works on online players only.
 */
public final class JailCommands {
    private static final SuggestionProvider<CommandSourceStack> JAILS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(EssData.get(ctx.getSource().getServer()).jails().keySet(), builder);

    private JailCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("setjail")
                .requires(PermissionNodes.SETJAIL.require())
                .then(Commands.argument("jail", StringArgumentType.word())
                        .executes(ctx -> {
                            String name = CommandUtil.validName(ctx.getSource(), StringArgumentType.getString(ctx, "jail"));
                            if (name == null) {
                                return 0;
                            }
                            StoredLocation here = StoredLocation.of(ctx.getSource().getPlayerOrException());
                            EssData.get(ctx.getSource().getServer()).setJail(name, here);
                            return CommandUtil.ok(ctx.getSource(), "jail.set", name);
                        })));
        dispatcher.register(Commands.literal("deljail")
                .requires(PermissionNodes.DELJAIL.require())
                .then(Commands.argument("jail", StringArgumentType.word())
                        .suggests(JAILS)
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "jail").toLowerCase(Locale.ROOT);
                            return EssData.get(ctx.getSource().getServer()).removeJail(name)
                                    ? CommandUtil.ok(ctx.getSource(), "jail.deleted", name)
                                    : CommandUtil.fail(ctx.getSource(), "jail.not-found", name);
                        })));
        dispatcher.register(Commands.literal("jails")
                .requires(PermissionNodes.JAILS.require())
                .executes(ctx -> {
                    var jails = EssData.get(ctx.getSource().getServer()).jails().keySet();
                    return jails.isEmpty() ? CommandUtil.fail(ctx.getSource(), "jail.none")
                            : CommandUtil.ok(ctx.getSource(), "jail.list", String.join(", ", jails));
                }));
        dispatcher.register(Commands.literal("jailedplayers")
                .requires(PermissionNodes.JAILEDPLAYERS.require())
                .executes(ctx -> {
                    List<String> names = EssData.get(ctx.getSource().getServer()).jailedNames();
                    return names.isEmpty() ? CommandUtil.ok(ctx.getSource(), "jail.nobody")
                            : CommandUtil.ok(ctx.getSource(), "jail.jailed", String.join(", ", names));
                }));
        LiteralCommandNode<CommandSourceStack> toggle = dispatcher.register(Commands.literal("togglejail")
                .requires(PermissionNodes.TOGGLEJAIL.require())
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> toggle(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), null, null))
                        .then(Commands.argument("jail", StringArgumentType.word())
                                .suggests(JAILS)
                                .executes(ctx -> toggle(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"),
                                        StringArgumentType.getString(ctx, "jail"), null))
                                .then(Commands.argument("duration", StringArgumentType.greedyString())
                                        .executes(ctx -> toggle(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"),
                                                StringArgumentType.getString(ctx, "jail"),
                                                StringArgumentType.getString(ctx, "duration")))))));
        for (String alias : List.of("jail", "tjail", "unjail")) {
            dispatcher.register(Commands.literal(alias).requires(toggle.getRequirement()).redirect(toggle));
        }
    }

    private static int toggle(CommandSourceStack source, ServerPlayer target, String jailName, String duration)
            throws CommandSyntaxException {
        String shown = target.getGameProfile().getName();
        if (JailService.isJailed(target)) {
            JailService.release(target);
            return CommandUtil.ok(source, "jail.freed", shown);
        }
        if (jailName == null) {
            return CommandUtil.fail(source, "jail.not-jailed", shown);
        }
        if (PermissionNodes.JAIL_EXEMPT.check(target)) {
            return CommandUtil.fail(source, "error.exempt", shown);
        }
        String name = jailName.toLowerCase(Locale.ROOT);
        StoredLocation jail = EssData.get(source.getServer()).jails().get(name);
        if (jail == null) {
            return CommandUtil.fail(source, "jail.not-found", name);
        }
        long until = -1;
        String length = "∞";
        if (duration != null) {
            Optional<Duration> parsed = DateDiff.parse(duration);
            if (parsed.isEmpty()) {
                return CommandUtil.fail(source, "error.invalid-duration", duration);
            }
            until = System.currentTimeMillis() + parsed.get().toMillis();
            length = DateDiff.format(parsed.get());
        }
        JailService.jail(target, name, jail, until);
        CommandUtil.tell(target, "jail.jailed-you", name, length);
        return CommandUtil.ok(source, "jail.done", shown, name, length);
    }
}
