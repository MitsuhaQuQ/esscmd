package com.mtmrails.esscmd.command;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.CommandNode;
import com.mtmrails.esscmd.compat.TabListNames;
import com.mtmrails.esscmd.data.EssData;
import com.mtmrails.esscmd.data.PlayerRecord;
import com.mtmrails.esscmd.message.Nicknames;
import com.mtmrails.esscmd.permission.PermissionNodes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

/** Persisted nicknames and reverse nickname lookup. */
public final class NickCommands {
    private NickCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandNode<CommandSourceStack> nick = dispatcher.register(Commands.literal("nick")
                .requires(PermissionNodes.NICK.require())
                .then(Commands.argument("nickname", StringArgumentType.word())
                        .executes(ctx -> set(ctx.getSource(), ctx.getSource().getPlayerOrException(),
                                StringArgumentType.getString(ctx, "nickname"))))
                .then(Commands.argument("player", EntityArgument.player())
                        .requires(PermissionNodes.NICK_OTHERS.require())
                        .then(Commands.argument("nickname", StringArgumentType.word())
                                .executes(ctx -> set(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"),
                                        StringArgumentType.getString(ctx, "nickname"))))));
        for (String alias : List.of("nickname")) {
            dispatcher.register(Commands.literal(alias).requires(PermissionNodes.NICK.require()).redirect(nick));
        }

        dispatcher.register(Commands.literal("realname")
                .requires(PermissionNodes.REALNAME.require())
                .then(Commands.argument("nickname", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                ctx.getSource().getOnlinePlayerNames(), builder))
                        .executes(ctx -> realName(ctx.getSource(),
                                StringArgumentType.getString(ctx, "nickname")))));
    }

    private static int set(CommandSourceStack source, ServerPlayer target, String value) {
        EssData data = EssData.get(source.getServer());
        String nickname = value.equalsIgnoreCase("off") ? "" : value;
        if (!nickname.isEmpty() && !Nicknames.valid(nickname)) {
            return CommandUtil.fail(source, "nick.invalid", value);
        }
        Optional<UUID> owner = data.findByNickname(nickname);
        if (!nickname.isEmpty() && owner.isPresent() && !owner.get().equals(target.getUUID())) {
            return CommandUtil.fail(source, "nick.taken", nickname);
        }
        data.setNickname(target.getUUID(), nickname);
        TabListNames.refresh(target);
        return CommandUtil.ok(source, nickname.isEmpty() ? "nick.off" : "nick.set",
                target.getGameProfile().getName(), nickname);
    }

    private static int realName(CommandSourceStack source, String nickname) {
        EssData data = EssData.get(source.getServer());
        String plain = nickname.startsWith("~") ? nickname.substring(1) : nickname;
        Optional<UUID> id = data.findByNickname(plain);
        if (id.isEmpty()) {
            return CommandUtil.fail(source, "realname.not-found", nickname);
        }
        ServerPlayer online = source.getServer().getPlayerList().getPlayer(id.get());
        String name = online != null ? online.getGameProfile().getName()
                : data.player(id.get()).map(PlayerRecord::lastName).orElse(id.get().toString());
        return CommandUtil.ok(source, "realname.result", plain, name);
    }
}
