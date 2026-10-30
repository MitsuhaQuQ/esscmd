package com.mtmrails.esscmd.command;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mtmrails.esscmd.compat.Players;
import com.mtmrails.esscmd.data.EssData;
import com.mtmrails.esscmd.data.PlayerRecord;
import com.mtmrails.esscmd.data.StoredLocation;
import com.mtmrails.esscmd.permission.PermissionNodes;
import com.mtmrails.esscmd.player.AfkService;
import com.mtmrails.esscmd.player.Sessions;
import com.mtmrails.esscmd.util.DateDiff;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.storage.LevelResource;

/**
 * {@code /seen <player>}, {@code /whois <player>}, {@code /playtime [player]}, {@code /ping}.
 * Names may be offline players the mod has seen before.
 */
public final class PlayerInfoCommands {
    private static final long TICK_MILLIS = 50;

    private static final SuggestionProvider<CommandSourceStack> ONLINE = (ctx, builder) ->
            SharedSuggestionProvider.suggest(ctx.getSource().getOnlinePlayerNames(), builder);

    private PlayerInfoCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("seen")
                .requires(PermissionNodes.SEEN.require())
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(ONLINE)
                        .executes(ctx -> seen(ctx.getSource(), StringArgumentType.getString(ctx, "player")))));
        dispatcher.register(Commands.literal("whois")
                .requires(PermissionNodes.WHOIS.require())
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(ONLINE)
                        .executes(ctx -> whois(ctx.getSource(), StringArgumentType.getString(ctx, "player")))));
        dispatcher.register(Commands.literal("playtime")
                .requires(PermissionNodes.PLAYTIME.require())
                .executes(ctx -> {
                    ServerPlayer self = ctx.getSource().getPlayerOrException();
                    return playtime(ctx.getSource(), self.getUUID(), self.getGameProfile().getName());
                })
                .then(Commands.argument("player", StringArgumentType.word())
                        .requires(PermissionNodes.PLAYTIME_OTHERS.require())
                        .suggests(ONLINE)
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "player");
                            Optional<UUID> id = Players.findId(ctx.getSource().getServer(), name);
                            if (id.isEmpty()) {
                                return CommandUtil.fail(ctx.getSource(), "error.player-not-found", name);
                            }
                            return playtime(ctx.getSource(), id.get(), displayName(ctx.getSource().getServer(), id.get(), name));
                        })));
        dispatcher.register(Commands.literal("ping")
                .requires(PermissionNodes.PING.require())
                .executes(ctx -> ctx.getSource().getEntity() instanceof ServerPlayer player
                        ? CommandUtil.ok(ctx.getSource(), "ping.latency", Players.latency(player))
                        : CommandUtil.ok(ctx.getSource(), "ping.pong")));
    }

    /** The stored account name if known, so offline names come out in their real case. */
    private static String displayName(MinecraftServer server, UUID id, String typed) {
        ServerPlayer online = server.getPlayerList().getPlayer(id);
        if (online != null) {
            return online.getGameProfile().getName();
        }
        return EssData.get(server).player(id).map(PlayerRecord::lastName).filter(n -> !n.isEmpty()).orElse(typed);
    }

    private static String ago(long epochMillis) {
        return DateDiff.format(Duration.ofMillis(System.currentTimeMillis() - epochMillis));
    }

    private static int seen(CommandSourceStack source, String name) {
        MinecraftServer server = source.getServer();
        ServerPlayer online = server.getPlayerList().getPlayerByName(name);
        if (online != null) {
            return CommandUtil.ok(source, "seen.online", online.getGameProfile().getName(), ago(Sessions.joinedAt(online)));
        }
        Optional<UUID> id = Players.findId(server, name);
        Optional<PlayerRecord> record = id.flatMap(uuid -> EssData.get(server).player(uuid));
        if (record.isEmpty() || record.get().lastSeen() == 0) {
            return CommandUtil.fail(source, "seen.never", name);
        }
        return CommandUtil.ok(source, "seen.offline", displayName(server, id.get(), name), ago(record.get().lastSeen()));
    }

    private static int whois(CommandSourceStack source, String name) {
        MinecraftServer server = source.getServer();
        Optional<UUID> id = Players.findId(server, name);
        if (id.isEmpty()) {
            return CommandUtil.fail(source, "error.player-not-found", name);
        }
        EssData data = EssData.get(server);
        ServerPlayer online = server.getPlayerList().getPlayer(id.get());
        String shown = displayName(server, id.get(), name);
        CommandUtil.ok(source, "whois.header", shown);
        CommandUtil.ok(source, "whois.uuid", id.get());
        if (online != null) {
            CommandUtil.ok(source, "whois.online", ago(Sessions.joinedAt(online)));
            CommandUtil.ok(source, "whois.location", StoredLocation.of(online).describe());
            CommandUtil.ok(source, "whois.health", Math.round(online.getHealth()), Math.round(online.getMaxHealth()),
                    online.getFoodData().getFoodLevel());
            CommandUtil.ok(source, "whois.gamemode", online.gameMode.getGameModeForPlayer().getName());
            CommandUtil.ok(source, "whois.flags", yesNo(server.getPlayerList().isOp(online.getGameProfile())),
                    yesNo(data.fly(id.get())), yesNo(data.god(id.get())), yesNo(AfkService.isAfk(online)));
            if (PermissionNodes.WHOIS_IP.check(source)) {
                CommandUtil.ok(source, "whois.ip", online.getIpAddress());
            }
        } else {
            Optional<PlayerRecord> record = data.player(id.get());
            if (record.isPresent() && record.get().lastSeen() > 0) {
                CommandUtil.ok(source, "whois.offline", ago(record.get().lastSeen()));
                StoredLocation logout = record.get().logoutLocation();
                if (logout != null) {
                    CommandUtil.ok(source, "whois.logout-location", logout.describe());
                }
            } else {
                CommandUtil.ok(source, "whois.offline-unknown");
            }
            CommandUtil.ok(source, "whois.flags-offline", yesNo(data.fly(id.get())), yesNo(data.god(id.get())));
        }
        return 1;
    }

    private static String yesNo(boolean value) {
        return value ? "✔" : "✘";
    }

    /** Online: from the live stats. Offline: read {@code <world>/stats/<uuid>.json} off the server thread. */
    private static int playtime(CommandSourceStack source, UUID id, String name) {
        MinecraftServer server = source.getServer();
        ServerPlayer online = server.getPlayerList().getPlayer(id);
        if (online != null) {
            int ticks = online.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
            return CommandUtil.ok(source, "playtime.result", name, DateDiff.format(Duration.ofMillis(ticks * TICK_MILLIS)));
        }
        Path file = server.getWorldPath(LevelResource.PLAYER_STATS_DIR).resolve(id + ".json");
        CompletableFuture.supplyAsync(() -> readPlayTime(file)).thenAcceptAsync(ticks -> {
            if (ticks.isPresent()) {
                CommandUtil.ok(source, "playtime.result", name, DateDiff.format(Duration.ofMillis(ticks.getAsLong() * TICK_MILLIS)));
            } else {
                CommandUtil.fail(source, "playtime.unknown", name);
            }
        }, server);
        return 1;
    }

    private static OptionalLong readPlayTime(Path file) {
        if (!Files.exists(file)) {
            return OptionalLong.empty();
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject custom = root.getAsJsonObject("stats").getAsJsonObject("minecraft:custom");
            JsonElement value = custom == null ? null : custom.get("minecraft:play_time");
            return value == null ? OptionalLong.empty() : OptionalLong.of(value.getAsLong());
        } catch (IOException | RuntimeException e) {
            return OptionalLong.empty();
        }
    }
}
