package com.mtmrails.esscmd.command;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mtmrails.esscmd.compat.Dimensions;
import com.mtmrails.esscmd.compat.Players;
import com.mtmrails.esscmd.compat.ServerStats;
import com.mtmrails.esscmd.compat.Texts;
import com.mtmrails.esscmd.config.ConfigManager;
import com.mtmrails.esscmd.data.EssData;
import com.mtmrails.esscmd.data.PlayerRecord;
import com.mtmrails.esscmd.message.Messages;
import com.mtmrails.esscmd.permission.PermissionNode;
import com.mtmrails.esscmd.permission.PermissionNodes;
import com.mtmrails.esscmd.util.DateDiff;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.UserBanListEntry;
import org.jetbrains.annotations.Nullable;

/**
 * {@code /mute}, {@code /tempban}, {@code /tempbanip}, {@code /unban}, {@code /unbanip},
 * {@code /kickall}, {@code /sudo}, {@code /gc}. Bans use the vanilla ban lists, so vanilla
 * {@code /banlist} and {@code /pardon} see them too. Offline players work if the mod knows them.
 */
public final class ModerationCommands {
    private static final Pattern IP = Pattern.compile("[0-9a-fA-F:.]*[.:][0-9a-fA-F:.]*");

    private static final SuggestionProvider<CommandSourceStack> ONLINE = (ctx, builder) ->
            SharedSuggestionProvider.suggest(ctx.getSource().getOnlinePlayerNames(), builder);

    private ModerationCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mute")
                .requires(PermissionNodes.MUTE.require())
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(ONLINE)
                        .executes(ctx -> mute(ctx.getSource(), StringArgumentType.getString(ctx, "player"), null))
                        .then(Commands.argument("details", StringArgumentType.greedyString())
                                .executes(ctx -> mute(ctx.getSource(), StringArgumentType.getString(ctx, "player"),
                                        StringArgumentType.getString(ctx, "details"))))));
        dispatcher.register(Commands.literal("tempban")
                .requires(PermissionNodes.TEMPBAN.require())
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(ONLINE)
                        .then(Commands.argument("details", StringArgumentType.greedyString())
                                .executes(ctx -> tempban(ctx.getSource(), StringArgumentType.getString(ctx, "player"),
                                        StringArgumentType.getString(ctx, "details"))))));
        dispatcher.register(Commands.literal("tempbanip")
                .requires(PermissionNodes.TEMPBANIP.require())
                .then(Commands.argument("target", StringArgumentType.string())
                        .suggests(ONLINE)
                        .then(Commands.argument("details", StringArgumentType.greedyString())
                                .executes(ctx -> tempbanIp(ctx.getSource(), StringArgumentType.getString(ctx, "target"),
                                        StringArgumentType.getString(ctx, "details"))))));
        dispatcher.register(Commands.literal("unban")
                .requires(PermissionNodes.UNBAN.require())
                .then(Commands.argument("player", StringArgumentType.word())
                        .executes(ctx -> unban(ctx.getSource(), StringArgumentType.getString(ctx, "player")))));
        dispatcher.register(Commands.literal("unbanip")
                .requires(PermissionNodes.UNBANIP.require())
                .then(Commands.argument("address", StringArgumentType.string())
                        .executes(ctx -> unbanIp(ctx.getSource(), StringArgumentType.getString(ctx, "address")))));
        dispatcher.register(Commands.literal("kickall")
                .requires(PermissionNodes.KICKALL.require())
                .executes(ctx -> kickAll(ctx.getSource(), null))
                .then(Commands.argument("reason", StringArgumentType.greedyString())
                        .executes(ctx -> kickAll(ctx.getSource(), StringArgumentType.getString(ctx, "reason")))));
        dispatcher.register(Commands.literal("sudo")
                .requires(PermissionNodes.SUDO.require())
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("command", StringArgumentType.greedyString())
                                .executes(ctx -> sudo(ctx.getSource(), dispatcher, EntityArgument.getPlayer(ctx, "player"),
                                        StringArgumentType.getString(ctx, "command"))))));
        dispatcher.register(Commands.literal("gc")
                .requires(PermissionNodes.GC.require())
                .executes(ctx -> gc(ctx.getSource())));
    }

    /** {@code [datediff] [reason...]}: the first word is a duration if it parses as one. */
    private record Details(@Nullable Duration duration, @Nullable String reason) {
        static Details parse(@Nullable String text) {
            if (text == null || text.isBlank()) {
                return new Details(null, null);
            }
            String[] parts = text.trim().split("\\s+", 2);
            Optional<Duration> duration = DateDiff.parse(parts[0]);
            if (duration.isPresent()) {
                return new Details(duration.get(), parts.length > 1 ? parts[1] : null);
            }
            return new Details(null, text.trim());
        }
    }

    /** A null duration means "forever", which exceeds any limit. */
    private static boolean overLimit(CommandSourceStack source, @Nullable Duration duration, long maxSeconds, PermissionNode unlimited) {
        if (maxSeconds < 0 || unlimited.check(source)) {
            return false;
        }
        return duration == null || duration.getSeconds() > maxSeconds;
    }

    private static boolean exempt(CommandSourceStack source, @Nullable ServerPlayer online, PermissionNode node) {
        return online != null && node.check(online);
    }

    private static String shownName(MinecraftServer server, UUID id, String typed) {
        return EssData.get(server).player(id).map(PlayerRecord::lastName).filter(n -> !n.isEmpty()).orElse(typed);
    }

    private static int mute(CommandSourceStack source, String name, @Nullable String details) {
        MinecraftServer server = source.getServer();
        Optional<UUID> id = Players.findId(server, name);
        if (id.isEmpty()) {
            return CommandUtil.fail(source, "error.player-not-found", name);
        }
        ServerPlayer online = server.getPlayerList().getPlayer(id.get());
        String shown = shownName(server, id.get(), name);
        EssData data = EssData.get(server);
        if (data.muted(id.get()) && details == null) {
            data.clearMute(id.get());
            if (online != null) {
                CommandUtil.tell(online, "mute.unmuted");
            }
            return CommandUtil.ok(source, "mute.removed", shown);
        }
        if (exempt(source, online, PermissionNodes.MUTE_EXEMPT)) {
            return CommandUtil.fail(source, "error.exempt", shown);
        }
        Details parsed = Details.parse(details);
        if (overLimit(source, parsed.duration(), ConfigManager.get().moderation.maxMuteSeconds, PermissionNodes.MUTE_UNLIMITED)) {
            return CommandUtil.fail(source, "error.duration-limit", DateDiff.format(Duration.ofSeconds(ConfigManager.get().moderation.maxMuteSeconds)));
        }
        long until = parsed.duration() == null ? -1 : System.currentTimeMillis() + parsed.duration().toMillis();
        data.setMute(id.get(), until, parsed.reason() == null ? "" : parsed.reason());
        String length = parsed.duration() == null ? "∞" : DateDiff.format(parsed.duration());
        if (online != null) {
            CommandUtil.tell(online, "mute.muted", length);
        }
        return CommandUtil.ok(source, "mute.done", shown, length);
    }

    private static int tempban(CommandSourceStack source, String name, String details) {
        MinecraftServer server = source.getServer();
        Optional<UUID> id = Players.findId(server, name);
        if (id.isEmpty()) {
            return CommandUtil.fail(source, "error.player-not-found", name);
        }
        Details parsed = Details.parse(details);
        if (parsed.duration() == null) {
            return CommandUtil.fail(source, "error.invalid-duration", details);
        }
        ServerPlayer online = server.getPlayerList().getPlayer(id.get());
        String shown = shownName(server, id.get(), name);
        if (exempt(source, online, PermissionNodes.TEMPBAN_EXEMPT)) {
            return CommandUtil.fail(source, "error.exempt", shown);
        }
        if (overLimit(source, parsed.duration(), ConfigManager.get().moderation.maxTempbanSeconds, PermissionNodes.TEMPBAN_UNLIMITED)) {
            return CommandUtil.fail(source, "error.duration-limit", DateDiff.format(Duration.ofSeconds(ConfigManager.get().moderation.maxTempbanSeconds)));
        }
        Date now = new Date();
        Date expires = new Date(now.getTime() + parsed.duration().toMillis());
        String reason = parsed.reason() != null ? parsed.reason() : Messages.raw(source, "ban.default-reason");
        server.getPlayerList().getBans().add(new UserBanListEntry(new GameProfile(id.get(), shown), now, source.getTextName(), expires, reason));
        if (online != null) {
            online.connection.disconnect(Messages.error(online, "ban.kicked", DateDiff.format(parsed.duration()), reason));
        }
        return CommandUtil.ok(source, "ban.done", shown, DateDiff.format(parsed.duration()));
    }

    private static int tempbanIp(CommandSourceStack source, String target, String details) {
        MinecraftServer server = source.getServer();
        ServerPlayer online = server.getPlayerList().getPlayerByName(target);
        String address = online != null ? online.getIpAddress() : target;
        if (online == null && !IP.matcher(address).matches()) {
            return CommandUtil.fail(source, "error.player-not-found", target);
        }
        Details parsed = Details.parse(details);
        if (parsed.duration() == null) {
            return CommandUtil.fail(source, "error.invalid-duration", details);
        }
        Date now = new Date();
        Date expires = new Date(now.getTime() + parsed.duration().toMillis());
        String reason = parsed.reason() != null ? parsed.reason() : Messages.raw(source, "ban.default-reason");
        server.getPlayerList().getIpBans().add(new IpBanListEntry(address, now, source.getTextName(), expires, reason));
        int kicked = 0;
        for (ServerPlayer player : List.copyOf(server.getPlayerList().getPlayers())) {
            if (address.equals(player.getIpAddress())) {
                player.connection.disconnect(Messages.error(player, "ban.kicked", DateDiff.format(parsed.duration()), reason));
                kicked++;
            }
        }
        return CommandUtil.ok(source, "ban.ip-done", address, DateDiff.format(parsed.duration()), kicked);
    }

    private static int unban(CommandSourceStack source, String name) {
        MinecraftServer server = source.getServer();
        Optional<UUID> id = Players.findId(server, name);
        if (id.isEmpty()) {
            return CommandUtil.fail(source, "error.player-not-found", name);
        }
        GameProfile profile = new GameProfile(id.get(), name);
        if (!server.getPlayerList().getBans().isBanned(profile)) {
            return CommandUtil.fail(source, "ban.not-banned", name);
        }
        server.getPlayerList().getBans().remove(profile);
        return CommandUtil.ok(source, "ban.removed", name);
    }

    private static int unbanIp(CommandSourceStack source, String address) {
        var bans = source.getServer().getPlayerList().getIpBans();
        if (bans.get(address) == null) {
            return CommandUtil.fail(source, "ban.not-banned", address);
        }
        bans.remove(address);
        return CommandUtil.ok(source, "ban.removed", address);
    }

    private static int kickAll(CommandSourceStack source, @Nullable String reason) {
        int kicked = 0;
        for (ServerPlayer player : List.copyOf(source.getServer().getPlayerList().getPlayers())) {
            if (player != source.getEntity()) {
                player.connection.disconnect(reason != null ? Texts.literal(reason)
                        : Messages.info(player, "kickall.reason"));
                kicked++;
            }
        }
        return CommandUtil.ok(source, "kickall.done", kicked);
    }

    private static int sudo(CommandSourceStack source, CommandDispatcher<CommandSourceStack> dispatcher,
                            ServerPlayer target, String command) {
        if (PermissionNodes.SUDO_EXEMPT.check(target)) {
            return CommandUtil.fail(source, "error.exempt", target.getGameProfile().getName());
        }
        String line = command.startsWith("/") ? command.substring(1) : command;
        try {
            dispatcher.execute(line, target.createCommandSourceStack());
        } catch (CommandSyntaxException e) {
            return CommandUtil.fail(source, "sudo.failed", e.getMessage());
        }
        return CommandUtil.ok(source, "sudo.done", target.getGameProfile().getName(), line);
    }

    private static int gc(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        Runtime runtime = Runtime.getRuntime();
        long mib = 1024 * 1024;
        double mspt = ServerStats.averageTickMillis(server);
        CommandUtil.ok(source, "gc.uptime", DateDiff.format(Duration.ofMillis(ManagementFactory.getRuntimeMXBean().getUptime())));
        CommandUtil.ok(source, "gc.tps", String.format("%.1f", Math.min(20.0, 1000.0 / Math.max(mspt, 0.001))), String.format("%.1f", mspt));
        CommandUtil.ok(source, "gc.memory", (runtime.totalMemory() - runtime.freeMemory()) / mib, runtime.maxMemory() / mib);
        for (ServerLevel level : server.getAllLevels()) {
            int entities = 0;
            for (var ignored : level.getAllEntities()) {
                entities++;
            }
            CommandUtil.ok(source, "gc.world", Dimensions.id(level.dimension()), level.getChunkSource().getLoadedChunksCount(), entities);
        }
        return 1;
    }
}
