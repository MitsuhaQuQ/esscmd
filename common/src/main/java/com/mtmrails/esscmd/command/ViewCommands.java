package com.mtmrails.esscmd.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mtmrails.esscmd.permission.PermissionNodes;
import com.mtmrails.esscmd.player.PlayerView;
import com.mtmrails.esscmd.util.TimeOfDay;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /ptime [list|reset|day|night|dawn|17:30|4pm|4000ticks] [player|*]} and
 * {@code /pweather [list|reset|storm|sun|clear] [player|*]}. Arguments are one greedy string,
 * because {@code 17:30} isn't a valid unquoted Brigadier word.
 */
public final class ViewCommands {
    private ViewCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ptime")
                .requires(PermissionNodes.PTIME.require())
                .executes(ctx -> list(ctx.getSource()))
                .then(Commands.argument("args", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                List.of("list", "reset", "day", "night", "dawn", "noon"), builder))
                        .executes(ctx -> ptime(ctx.getSource(), StringArgumentType.getString(ctx, "args").trim().split("\\s+")))));
        dispatcher.register(Commands.literal("pweather")
                .requires(PermissionNodes.PWEATHER.require())
                .executes(ctx -> list(ctx.getSource()))
                .then(Commands.argument("args", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                List.of("list", "reset", "storm", "sun", "clear"), builder))
                        .executes(ctx -> pweather(ctx.getSource(), StringArgumentType.getString(ctx, "args").trim().split("\\s+")))));
    }

    /** The optional second word: a player, {@code *}, or the source itself. Null after an error. */
    private static List<ServerPlayer> targets(CommandSourceStack source, String[] args) throws CommandSyntaxException {
        if (args.length < 2) {
            return List.of(source.getPlayerOrException());
        }
        if (!PermissionNodes.PTIME_OTHERS.check(source)) {
            CommandUtil.fail(source, "error.no-permission-others");
            return null;
        }
        if (args[1].equals("*")) {
            return new ArrayList<>(source.getServer().getPlayerList().getPlayers());
        }
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(args[1]);
        if (player == null) {
            CommandUtil.fail(source, "error.player-not-found", args[1]);
            return null;
        }
        return List.of(player);
    }

    private static int list(CommandSourceStack source) {
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            Long time = PlayerView.time(player);
            Boolean weather = PlayerView.weather(player);
            if (time != null || weather != null) {
                CommandUtil.ok(source, "view.entry", player.getGameProfile().getName(),
                        time == null ? "-" : time + "t", weather == null ? "-" : weather ? "storm" : "sun");
            }
        }
        return 1;
    }

    private static int ptime(CommandSourceStack source, String[] args) throws CommandSyntaxException {
        if (args[0].equalsIgnoreCase("list")) {
            return list(source);
        }
        List<ServerPlayer> targets = targets(source, args);
        if (targets == null) {
            return 0;
        }
        if (args[0].equalsIgnoreCase("reset")) {
            targets.forEach(PlayerView::resetTime);
            return CommandUtil.ok(source, "ptime.reset", targets.size());
        }
        OptionalLong time = TimeOfDay.parse(args[0]);
        if (time.isEmpty()) {
            return CommandUtil.fail(source, "ptime.invalid", args[0]);
        }
        targets.forEach(player -> PlayerView.setTime(player, time.getAsLong()));
        return CommandUtil.ok(source, "ptime.set", time.getAsLong(), targets.size());
    }

    private static int pweather(CommandSourceStack source, String[] args) throws CommandSyntaxException {
        String value = args[0].toLowerCase(Locale.ROOT);
        if (value.equals("list")) {
            return list(source);
        }
        List<ServerPlayer> targets = targets(source, args);
        if (targets == null) {
            return 0;
        }
        switch (value) {
            case "reset" -> targets.forEach(PlayerView::resetWeather);
            case "storm" -> targets.forEach(player -> PlayerView.setWeather(player, true));
            case "sun", "clear" -> targets.forEach(player -> PlayerView.setWeather(player, false));
            default -> {
                return CommandUtil.fail(source, "pweather.invalid", args[0]);
            }
        }
        return CommandUtil.ok(source, "pweather.set", value, targets.size());
    }
}
