package com.mtmrails.esscmd.command;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.CommandNode;
import com.mtmrails.esscmd.compat.Players;
import com.mtmrails.esscmd.config.ConfigManager;
import com.mtmrails.esscmd.data.EssData;
import com.mtmrails.esscmd.data.MailMessage;
import com.mtmrails.esscmd.data.PlayerRecord;
import com.mtmrails.esscmd.message.Messages;
import com.mtmrails.esscmd.permission.PermissionNodes;
import com.mtmrails.esscmd.player.Mutes;
import com.mtmrails.esscmd.util.DateDiff;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Persisted player mail, including expiring mail and administrative clearing. */
public final class MailCommands {
    private static final Map<UUID, Deque<Long>> RECENT_SENDS = new HashMap<>();

    private MailCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandNode<CommandSourceStack> mail = dispatcher.register(Commands.literal("mail")
                .requires(PermissionNodes.MAIL.require())
                .executes(ctx -> status(ctx.getSource()))
                .then(Commands.literal("read").executes(ctx -> read(ctx.getSource())))
                .then(Commands.literal("clear")
                        .executes(ctx -> clearSelf(ctx.getSource(), null))
                        .then(Commands.argument("number", IntegerArgumentType.integer(1))
                                .executes(ctx -> clearSelf(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "number"))))
                        .then(Commands.argument("player", StringArgumentType.word())
                                .requires(PermissionNodes.MAIL_OTHERS.require())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        ctx.getSource().getOnlinePlayerNames(), builder))
                                .executes(ctx -> clearOther(ctx.getSource(), StringArgumentType.getString(ctx, "player"), null))
                                .then(Commands.argument("number", IntegerArgumentType.integer(1))
                                        .executes(ctx -> clearOther(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "player"),
                                                IntegerArgumentType.getInteger(ctx, "number"))))))
                .then(Commands.literal("send")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        ctx.getSource().getOnlinePlayerNames(), builder))
                                .then(Commands.argument("message", StringArgumentType.greedyString())
                                        .executes(ctx -> send(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "player"),
                                                StringArgumentType.getString(ctx, "message"), 0)))))
                .then(Commands.literal("sendtemp")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        ctx.getSource().getOnlinePlayerNames(), builder))
                                .then(Commands.argument("expiry", StringArgumentType.word())
                                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                                .executes(ctx -> sendTemporary(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "expiry"),
                                                        StringArgumentType.getString(ctx, "message")))))))
                .then(Commands.literal("sendall")
                        .requires(PermissionNodes.MAIL_SENDALL.require())
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(ctx -> sendAll(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "message"))))));
        for (String alias : List.of("memo", "email")) {
            dispatcher.register(Commands.literal(alias).requires(PermissionNodes.MAIL.require())
                    .executes(mail.getCommand()).redirect(mail));
        }
    }

    private static int status(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        EssData data = EssData.get(source.getServer());
        List<MailMessage> mail = data.mail(player.getUUID(), System.currentTimeMillis());
        long unread = mail.stream().filter(message -> !message.read()).count();
        return mail.isEmpty() ? CommandUtil.ok(source, "mail.none")
                : CommandUtil.ok(source, "mail.status", unread, mail.size());
    }

    private static int read(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        EssData data = EssData.get(source.getServer());
        List<MailMessage> mail = data.mail(player.getUUID(), System.currentTimeMillis());
        if (mail.isEmpty()) {
            return CommandUtil.ok(source, "mail.none");
        }
        for (int i = 0; i < mail.size(); i++) {
            MailMessage message = mail.get(i);
            String ago = DateDiff.format(Duration.between(Instant.ofEpochMilli(message.sentAt()), Instant.now()));
            CommandUtil.ok(source, "mail.entry", i + 1, message.senderName(), ago, message.text());
        }
        data.markMailRead(player.getUUID());
        return mail.size();
    }

    private static int clearSelf(CommandSourceStack source, Integer number)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return clear(source, source.getPlayerOrException().getUUID(), number);
    }

    private static int clearOther(CommandSourceStack source, String name, Integer number) {
        Optional<UUID> id = Players.findId(source.getServer(), name);
        if (id.isEmpty()) {
            return CommandUtil.fail(source, "error.player-not-found", name);
        }
        return clear(source, id.get(), number);
    }

    private static int clear(CommandSourceStack source, UUID player, Integer number) {
        EssData data = EssData.get(source.getServer());
        data.mail(player, System.currentTimeMillis());
        if (number != null) {
            return data.removeMail(player, number) ? CommandUtil.ok(source, "mail.cleared-one", number)
                    : CommandUtil.fail(source, "mail.not-found", number);
        }
        return CommandUtil.ok(source, "mail.cleared", data.clearMail(player));
    }

    private static int sendTemporary(CommandSourceStack source, String target, String expiry, String text) {
        Optional<Duration> duration = DateDiff.parse(expiry);
        if (duration.isEmpty()) {
            return CommandUtil.fail(source, "mail.invalid-expiry", expiry);
        }
        long expires;
        try {
            expires = Math.addExact(System.currentTimeMillis(), duration.get().toMillis());
        } catch (ArithmeticException e) {
            return CommandUtil.fail(source, "mail.invalid-expiry", expiry);
        }
        return send(source, target, text, expires);
    }

    /** mail.perMinute: a sliding one-minute window per sender. */
    private static boolean withinRateLimit(ServerPlayer sender) {
        long now = System.currentTimeMillis();
        Deque<Long> times = RECENT_SENDS.computeIfAbsent(sender.getUUID(), id -> new ArrayDeque<>());
        while (!times.isEmpty() && now - times.peekFirst() >= 60_000L) {
            times.removeFirst();
        }
        if (times.size() >= ConfigManager.get().mail.perMinute) {
            return false;
        }
        times.addLast(now);
        return true;
    }

    private static int send(CommandSourceStack source, String targetName, String text, long expiresAt) {
        if (source.getEntity() instanceof ServerPlayer sender && Mutes.blockChat(sender)) {
            return 0;
        }
        if (!(source.getEntity() instanceof ServerPlayer sender)) {
            return CommandUtil.fail(source, "mail.player-only");
        }
        if (!withinRateLimit(sender)) {
            return CommandUtil.fail(source, "mail.rate-limit");
        }
        MinecraftServer server = source.getServer();
        Optional<UUID> target = Players.findId(server, targetName);
        if (target.isEmpty()) {
            return CommandUtil.fail(source, "error.player-not-found", targetName);
        }
        EssData data = EssData.get(server);
        if (data.ignores(target.get(), sender.getUUID())) {
            return CommandUtil.fail(source, "mail.ignored", targetName);
        }
        String shown = data.player(target.get()).map(PlayerRecord::lastName).filter(name -> !name.isEmpty())
                .orElse(targetName);
        data.addMail(target.get(), new MailMessage(sender.getGameProfile().getName(), text,
                System.currentTimeMillis(), expiresAt, false));
        ServerPlayer online = server.getPlayerList().getPlayer(target.get());
        if (online != null) {
            CommandUtil.tell(online, "mail.received", sender.getGameProfile().getName());
        }
        MessagingCommands.spyMail(server.getPlayerList().getPlayers(), sender, target.get(), shown, text);
        return CommandUtil.ok(source, "mail.sent", shown);
    }

    private static int sendAll(CommandSourceStack source, String text)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sender = source.getPlayerOrException();
        EssData data = EssData.get(source.getServer());
        int count = 0;
        for (UUID target : data.knownPlayers()) {
            if (!target.equals(sender.getUUID()) && !data.ignores(target, sender.getUUID())) {
                data.addMail(target, new MailMessage(sender.getGameProfile().getName(), text,
                        System.currentTimeMillis(), 0, false));
                ServerPlayer online = source.getServer().getPlayerList().getPlayer(target);
                if (online != null) {
                    CommandUtil.tell(online, "mail.received", sender.getGameProfile().getName());
                }
                count++;
            }
        }
        MessagingCommands.spyMailAll(source.getServer().getPlayerList().getPlayers(), sender, text);
        return CommandUtil.ok(source, "mail.sent-all", count);
    }
}
