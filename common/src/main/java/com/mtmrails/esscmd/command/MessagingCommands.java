package com.mtmrails.esscmd.command;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.mtmrails.esscmd.compat.Players;
import com.mtmrails.esscmd.compat.CommandTrees;
import com.mtmrails.esscmd.config.ConfigManager;
import com.mtmrails.esscmd.data.EssData;
import com.mtmrails.esscmd.message.Messages;
import com.mtmrails.esscmd.message.MessagingService;
import com.mtmrails.esscmd.message.Nicknames;
import com.mtmrails.esscmd.permission.PermissionNode;
import com.mtmrails.esscmd.permission.PermissionNodes;
import com.mtmrails.esscmd.player.Mutes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/** Private messages, reply history, ignore, social spy and helpop. */
public final class MessagingCommands {
    private static final Set<String> VANILLA_NAMES = Set.of("msg", "tell", "w");

    private MessagingCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        List<String> overrides = ConfigManager.get().overrideVanilla.stream()
                .map(name -> name.toLowerCase(Locale.ROOT)).filter(VANILLA_NAMES::contains).distinct().toList();
        overrides.forEach(name -> removeRoot(dispatcher, name));

        CommandNode<CommandSourceStack> msg = dispatcher.register(messageLiteral("emsg"));
        if (overrides.contains("msg")) {
            msg = dispatcher.register(messageLiteral("msg"));
        }
        for (String alias : List.of("m", "t", "pm", "whisper")) {
            dispatcher.register(Commands.literal(alias).requires(PermissionNodes.MSG.require()).redirect(msg));
        }
        for (String alias : List.of("tell", "w")) {
            if (overrides.contains(alias)) {
                dispatcher.register(Commands.literal(alias).requires(PermissionNodes.MSG.require()).redirect(msg));
            }
        }

        CommandNode<CommandSourceStack> reply = dispatcher.register(Commands.literal("r")
                .requires(PermissionNodes.REPLY.require())
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> reply(ctx.getSource(), StringArgumentType.getString(ctx, "message")))));
        dispatcher.register(Commands.literal("reply").requires(PermissionNodes.REPLY.require()).redirect(reply));
        dispatcher.register(toggle("msgtoggle", PermissionNodes.MSGTOGGLE, PermissionNodes.MSGTOGGLE_OTHERS, false));
        CommandNode<CommandSourceStack> replyToggle = dispatcher.register(
                toggle("rtoggle", PermissionNodes.RTOGGLE, PermissionNodes.RTOGGLE_OTHERS, true));
        dispatcher.register(Commands.literal("replytoggle").requires(PermissionNodes.RTOGGLE.require())
                .redirect(replyToggle));
        CommandNode<CommandSourceStack> ignore = dispatcher.register(Commands.literal("ignore")
                .requires(PermissionNodes.IGNORE.require())
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> ignore(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))));
        for (String alias : List.of("rmignore", "unignore", "delignore")) {
            dispatcher.register(Commands.literal(alias).requires(PermissionNodes.IGNORE.require()).redirect(ignore));
        }
        dispatcher.register(toggle("socialspy", PermissionNodes.SOCIALSPY, PermissionNodes.SOCIALSPY_OTHERS, null));
        CommandNode<CommandSourceStack> helpop = dispatcher.register(Commands.literal("helpop")
                .requires(PermissionNodes.HELPOP.require())
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> helpop(ctx.getSource(), StringArgumentType.getString(ctx, "message")))));
        for (String alias : List.of("ac", "amsg")) {
            dispatcher.register(Commands.literal(alias).requires(PermissionNodes.HELPOP.require()).redirect(helpop));
        }
    }

    private static LiteralArgumentBuilder<CommandSourceStack> messageLiteral(String name) {
        return Commands.literal(name).requires(PermissionNodes.MSG.require())
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(ctx -> send(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"),
                                        StringArgumentType.getString(ctx, "message")))));
    }

    @SuppressWarnings("unchecked")
    private static void removeRoot(CommandDispatcher<CommandSourceStack> dispatcher, String name) {
        CommandTrees.removeRootCommand(dispatcher, name);
    }

    private static int send(CommandSourceStack source, ServerPlayer target, String text) throws CommandSyntaxException {
        return send(source, source.getPlayerOrException(), target, text);
    }

    private static int send(CommandSourceStack source, ServerPlayer sender, ServerPlayer target, String text) {
        if (Mutes.blockChat(sender)) {
            return 0;
        }
        if (sender == target) {
            return CommandUtil.fail(source, "msg.self");
        }
        EssData data = EssData.get(source.getServer());
        if (data.messagesDisabled(target.getUUID())) {
            return CommandUtil.fail(source, "msg.disabled", target.getGameProfile().getName());
        }
        if (data.ignores(target.getUUID(), sender.getUUID())) {
            return CommandUtil.fail(source, "msg.ignored", target.getGameProfile().getName());
        }
        String senderName = sender.getGameProfile().getName();
        String targetName = target.getGameProfile().getName();
        Players.send(sender, Messages.info(sender, "msg.outgoing", Nicknames.displayName(target), text));
        Players.send(target, Messages.info(target, "msg.incoming", Nicknames.displayName(sender), text));
        MessagingService.sent(sender.getUUID(), target.getUUID());
        spy(source.getServer().getPlayerList().getPlayers(), sender, target, "socialspy.msg", senderName, targetName, text);
        return 1;
    }

    private static int reply(CommandSourceStack source, String text) throws CommandSyntaxException {
        ServerPlayer sender = source.getPlayerOrException();
        EssData data = EssData.get(source.getServer());
        UUID id = MessagingService.replyTarget(sender.getUUID(), data.replyToRecipient(sender.getUUID()));
        if (id == null) {
            return CommandUtil.fail(source, "reply.none");
        }
        ServerPlayer target = source.getServer().getPlayerList().getPlayer(id);
        if (target == null) {
            return CommandUtil.fail(source, "reply.offline");
        }
        return send(source, sender, target, text);
    }

    private static int ignore(CommandSourceStack source, ServerPlayer target) throws CommandSyntaxException {
        ServerPlayer self = source.getPlayerOrException();
        if (self == target) {
            return CommandUtil.fail(source, "ignore.self");
        }
        boolean ignored = EssData.get(source.getServer()).toggleIgnored(self.getUUID(), target.getUUID());
        return CommandUtil.ok(source, ignored ? "ignore.on" : "ignore.off", target.getGameProfile().getName());
    }

    /** kind: false=messages enabled, true=reply-to-recipient, null=social spy. */
    private static LiteralArgumentBuilder<CommandSourceStack> toggle(String command, PermissionNode own,
                                                                     PermissionNode others, @Nullable Boolean kind) {
        return Commands.literal(command).requires(own.require())
                .executes(ctx -> setToggle(ctx.getSource(), ctx.getSource().getPlayerOrException(), null, kind))
                .then(Commands.literal("on").executes(ctx ->
                        setToggle(ctx.getSource(), ctx.getSource().getPlayerOrException(), true, kind)))
                .then(Commands.literal("off").executes(ctx ->
                        setToggle(ctx.getSource(), ctx.getSource().getPlayerOrException(), false, kind)))
                .then(Commands.argument("player", EntityArgument.player()).requires(others.require())
                        .executes(ctx -> setToggle(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), null, kind))
                        .then(Commands.literal("on").executes(ctx ->
                                setToggle(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), true, kind)))
                        .then(Commands.literal("off").executes(ctx ->
                                setToggle(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), false, kind))));
    }

    private static int setToggle(CommandSourceStack source, ServerPlayer player, @Nullable Boolean enabled,
                                 @Nullable Boolean kind) {
        EssData data = EssData.get(source.getServer());
        UUID id = player.getUUID();
        boolean current = kind == null ? data.socialSpy(id) : kind ? data.replyToRecipient(id) : !data.messagesDisabled(id);
        boolean next = enabled == null ? !current : enabled;
        String key;
        if (kind == null) {
            data.setSocialSpy(id, next);
            key = "socialspy.";
        } else if (kind) {
            data.setReplyToRecipient(id, next);
            key = "rtoggle.";
        } else {
            data.setMessagesDisabled(id, !next);
            key = "msgtoggle.";
        }
        return CommandUtil.ok(source, key + (next ? "on" : "off"), player.getGameProfile().getName());
    }

    private static int helpop(CommandSourceStack source, String text) throws CommandSyntaxException {
        ServerPlayer sender = source.getPlayerOrException();
        int recipients = 0;
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            if (PermissionNodes.HELPOP_RECEIVE.check(player)) {
                Players.send(player, Messages.info(player, "helpop.message", sender.getGameProfile().getName(), text));
                recipients++;
            }
        }
        return recipients == 0 ? CommandUtil.fail(source, "helpop.none") : CommandUtil.ok(source, "helpop.sent");
    }

    public static void spyMail(Iterable<ServerPlayer> players, ServerPlayer sender, UUID target, String targetName,
                               String text) {
        for (ServerPlayer spy : players) {
            if (spy != sender && !spy.getUUID().equals(target) && EssData.get(spy.server).socialSpy(spy.getUUID())) {
                Players.send(spy, Messages.info(spy, "socialspy.mail", sender.getGameProfile().getName(), targetName, text));
            }
        }
    }

    public static void spyMailAll(Iterable<ServerPlayer> players, ServerPlayer sender, String text) {
        for (ServerPlayer spy : players) {
            if (spy != sender && EssData.get(spy.server).socialSpy(spy.getUUID())) {
                Players.send(spy, Messages.info(spy, "socialspy.mail-all", sender.getGameProfile().getName(), text));
            }
        }
    }

    private static void spy(Iterable<ServerPlayer> players, ServerPlayer sender, ServerPlayer target,
                            String key, Object... args) {
        for (ServerPlayer player : players) {
            if (player != sender && player != target && EssData.get(player.server).socialSpy(player.getUUID())) {
                Players.send(player, Messages.info(player, key, args));
            }
        }
    }
}
