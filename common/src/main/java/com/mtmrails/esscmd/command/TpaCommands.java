package com.mtmrails.esscmd.command;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mtmrails.esscmd.compat.Players;
import com.mtmrails.esscmd.compat.Texts;
import com.mtmrails.esscmd.config.ConfigManager;
import com.mtmrails.esscmd.data.EssData;
import com.mtmrails.esscmd.data.StoredLocation;
import com.mtmrails.esscmd.message.Messages;
import com.mtmrails.esscmd.permission.PermissionNode;
import com.mtmrails.esscmd.permission.PermissionNodes;
import com.mtmrails.esscmd.teleport.TeleportService;
import com.mtmrails.esscmd.teleport.TpaRequests;
import com.mtmrails.esscmd.teleport.TpaRequests.Request;
import com.mtmrails.esscmd.teleport.TpaRequests.Type;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * {@code /tpa}, {@code /tpahere}, {@code /tpaccept}, {@code /tpdeny}, {@code /tpacancel},
 * {@code /tptoggle}, {@code /tpauto}.
 */
public final class TpaCommands {
    private TpaCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tpa")
                .requires(PermissionNodes.TPA.require())
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> request(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), Type.TO_TARGET))));
        dispatcher.register(Commands.literal("tpahere")
                .requires(PermissionNodes.TPAHERE.require())
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> request(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), Type.HERE))));
        dispatcher.register(Commands.literal("tpaccept")
                .requires(PermissionNodes.TPACCEPT.require())
                .executes(ctx -> accept(ctx.getSource(), null))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> accept(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))));
        dispatcher.register(Commands.literal("tpdeny")
                .requires(PermissionNodes.TPDENY.require())
                .executes(ctx -> deny(ctx.getSource(), null))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> deny(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))));
        dispatcher.register(Commands.literal("tpacancel")
                .requires(PermissionNodes.TPACANCEL.require())
                .executes(ctx -> cancel(ctx.getSource(), null))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> cancel(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))));
        dispatcher.register(toggle("tptoggle", PermissionNodes.TPTOGGLE, PermissionNodes.TPTOGGLE_OTHERS, false));
        dispatcher.register(toggle("tpauto", PermissionNodes.TPAUTO, PermissionNodes.TPAUTO_OTHERS, true));
    }

    private static String name(ServerPlayer player) {
        return player.getGameProfile().getName();
    }

    private static int request(CommandSourceStack source, ServerPlayer target, Type type) throws CommandSyntaxException {
        ServerPlayer requester = source.getPlayerOrException();
        if (requester == target) {
            return CommandUtil.fail(source, "tpa.self");
        }
        EssData data = EssData.get(source.getServer());
        if (data.ignores(target.getUUID(), requester.getUUID())) {
            return CommandUtil.fail(source, "tpa.ignored", name(target));
        }
        if (data.teleportDisabled(target.getUUID())) {
            return CommandUtil.fail(source, "tpa.disabled", name(target));
        }
        if (data.teleportAuto(target.getUUID())) {
            CommandUtil.tell(target, "tpa.auto-accepted", name(requester));
            CommandUtil.ok(source, "tpa.accepted-requester", name(target));
            return startTeleport(source.getServer(), requester.getUUID(), target.getUUID(), type) ? 1 : 0;
        }
        int timeout = ConfigManager.get().tpa.timeoutSeconds;
        TpaRequests.add(new Request(requester.getUUID(), name(requester), target.getUUID(), name(target), type,
                source.getServer().getTickCount() + timeout * 20));

        notifyTarget(target, name(requester), type, timeout);
        return CommandUtil.ok(source, "tpa.sent", name(target));
    }

    /** The request text plus clickable [Accept] [Deny] buttons, in the target's language. */
    static void notifyTarget(ServerPlayer target, String requesterName, Type type, int timeoutSeconds) {
        Players.send(target, Messages.info(target, type == Type.HERE ? "tpa.request-here" : "tpa.request", requesterName));
        Players.send(target, Texts.empty()
                .append(button(target, "tpa.accept-button", "/tpaccept " + requesterName, ChatFormatting.GREEN))
                .append(Texts.literal(" "))
                .append(button(target, "tpa.deny-button", "/tpdeny " + requesterName, ChatFormatting.RED))
                .append(Texts.literal(" "))
                .append(Messages.info(target, "tpa.expires", timeoutSeconds)));
    }

    private static MutableComponent button(ServerPlayer viewer, String key, String command, ChatFormatting color) {
        return Texts.runOnClick(Texts.literal(Messages.raw(viewer, key)).withStyle(color, ChatFormatting.BOLD),
                command, Texts.literal(command));
    }

    private static int accept(CommandSourceStack source, @Nullable ServerPlayer from) throws CommandSyntaxException {
        ServerPlayer target = source.getPlayerOrException();
        Request request = TpaRequests.take(target.getUUID(), from == null ? null : from.getUUID());
        if (request == null) {
            return CommandUtil.fail(source, "tpa.none");
        }
        ServerPlayer requester = source.getServer().getPlayerList().getPlayer(request.requester());
        if (requester == null) {
            return CommandUtil.fail(source, "tpa.offline", request.requesterName());
        }
        CommandUtil.tell(requester, "tpa.accepted-requester", name(target));
        CommandUtil.ok(source, "tpa.accepted");
        return startTeleport(source.getServer(), request.requester(), request.target(), request.type()) ? 1 : 0;
    }

    /** Moves the traveller (requester for /tpa, target for /tpahere) to wherever the other player is when it happens. */
    private static boolean startTeleport(MinecraftServer server, UUID requester, UUID target, Type type) {
        UUID traveller = type == Type.HERE ? target : requester;
        UUID destination = type == Type.HERE ? requester : target;
        ServerPlayer travellerPlayer = server.getPlayerList().getPlayer(traveller);
        if (travellerPlayer == null) {
            return false;
        }
        Supplier<StoredLocation> where = () -> {
            ServerPlayer player = server.getPlayerList().getPlayer(destination);
            return player == null ? null : StoredLocation.of(player);
        };
        return TeleportService.teleport(travellerPlayer, where);
    }

    private static int deny(CommandSourceStack source, @Nullable ServerPlayer from) throws CommandSyntaxException {
        ServerPlayer target = source.getPlayerOrException();
        Request request = TpaRequests.take(target.getUUID(), from == null ? null : from.getUUID());
        if (request == null) {
            return CommandUtil.fail(source, "tpa.none");
        }
        ServerPlayer requester = source.getServer().getPlayerList().getPlayer(request.requester());
        if (requester != null) {
            Players.send(requester, Messages.error(requester, "tpa.denied-requester", name(target)));
        }
        return CommandUtil.ok(source, "tpa.denied");
    }

    private static int cancel(CommandSourceStack source, @Nullable ServerPlayer to) throws CommandSyntaxException {
        ServerPlayer requester = source.getPlayerOrException();
        List<Request> cancelled = TpaRequests.cancelFrom(requester.getUUID(), to == null ? null : to.getUUID());
        if (cancelled.isEmpty()) {
            return CommandUtil.fail(source, "tpa.none-sent");
        }
        for (Request request : cancelled) {
            ServerPlayer target = source.getServer().getPlayerList().getPlayer(request.target());
            if (target != null) {
                CommandUtil.tell(target, "tpa.cancelled-target", name(requester));
            }
        }
        return CommandUtil.ok(source, "tpa.cancelled", cancelled.size());
    }

    /** {@code /<name> [player] [on|off]}; without on/off it flips the current state. */
    private static LiteralArgumentBuilder<CommandSourceStack> toggle(String command, PermissionNode node,
                                                                     PermissionNode others, boolean auto) {
        return Commands.literal(command)
                .requires(node.require())
                .executes(ctx -> setToggle(ctx.getSource(), ctx.getSource().getPlayerOrException(), null, auto))
                .then(Commands.literal("on").executes(ctx ->
                        setToggle(ctx.getSource(), ctx.getSource().getPlayerOrException(), true, auto)))
                .then(Commands.literal("off").executes(ctx ->
                        setToggle(ctx.getSource(), ctx.getSource().getPlayerOrException(), false, auto)))
                .then(Commands.argument("player", EntityArgument.player())
                        .requires(others.require())
                        .executes(ctx -> setToggle(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), null, auto))
                        .then(Commands.literal("on").executes(ctx ->
                                setToggle(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), true, auto)))
                        .then(Commands.literal("off").executes(ctx ->
                                setToggle(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), false, auto))));
    }

    /** {@code enabled} means "accepts requests" for /tptoggle and "auto-accepts" for /tpauto. */
    private static int setToggle(CommandSourceStack source, ServerPlayer player, @Nullable Boolean enabled, boolean auto) {
        EssData data = EssData.get(source.getServer());
        UUID id = player.getUUID();
        boolean current = auto ? data.teleportAuto(id) : !data.teleportDisabled(id);
        boolean next = enabled != null ? enabled : !current;
        if (auto) {
            data.setTeleportAuto(id, next);
        } else {
            data.setTeleportDisabled(id, !next);
        }
        String key = (auto ? "tpauto." : "tptoggle.") + (next ? "on" : "off");
        return CommandUtil.ok(source, key, name(player));
    }
}
