package com.mtmrails.esscmd.command;

import java.util.List;
import java.util.regex.Pattern;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mtmrails.esscmd.compat.Players;
import com.mtmrails.esscmd.compat.Texts;
import com.mtmrails.esscmd.message.TextDocument;
import com.mtmrails.esscmd.message.TextFiles;
import com.mtmrails.esscmd.permission.PermissionNode;
import com.mtmrails.esscmd.permission.PermissionNodes;
import com.mtmrails.esscmd.util.Pager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * {@code /motd}, {@code /rules}, {@code /info}, each {@code [chapter|page] [page]} like
 * Essentials. Lines may use {@code &} colour codes and {@code {player}}, {@code {online}},
 * {@code {max}} placeholders.
 */
public final class TextCommands {
    private static final int LINES_PER_PAGE = 9;
    private static final Pattern PAGE_NUMBER = Pattern.compile("[1-9][0-9]{0,3}");
    private static final Pattern COLOR_CODE = Pattern.compile("&([0-9a-fk-orA-FK-OR])");

    private TextCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        register(dispatcher, TextFiles.MOTD, PermissionNodes.MOTD);
        register(dispatcher, TextFiles.RULES, PermissionNodes.RULES);
        register(dispatcher, TextFiles.INFO, PermissionNodes.INFO);
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher, String name, PermissionNode node) {
        dispatcher.register(Commands.literal(name)
                .requires(node.require())
                .executes(ctx -> show(ctx.getSource(), name, null, 1))
                .then(Commands.argument("chapter", StringArgumentType.word())
                        .executes(ctx -> {
                            String arg = StringArgumentType.getString(ctx, "chapter");
                            return PAGE_NUMBER.matcher(arg).matches()
                                    ? show(ctx.getSource(), name, null, Integer.parseInt(arg))
                                    : show(ctx.getSource(), name, arg, 1);
                        })
                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                .executes(ctx -> show(ctx.getSource(), name, StringArgumentType.getString(ctx, "chapter"),
                                        IntegerArgumentType.getInteger(ctx, "page"))))));
    }

    private static int show(CommandSourceStack source, String name, @Nullable String chapter, int page) {
        TextDocument doc = TextFiles.get(source, name);
        List<String> lines = doc == null ? null : doc.chapter(chapter);
        if (lines == null) {
            return chapter == null ? CommandUtil.fail(source, "text.missing", name)
                    : CommandUtil.fail(source, "text.no-chapter", chapter);
        }
        String viewer = source.getEntity() instanceof ServerPlayer player ? player.getGameProfile().getName() : "Server";
        Pager.Page<String> shown = Pager.page(lines, page, LINES_PER_PAGE);
        for (String line : shown.items()) {
            Texts.success(source, render(line, source.getServer(), viewer), false);
        }
        if (shown.pages() > 1) {
            String next = "/" + name + (chapter == null ? "" : " " + chapter) + " " + (shown.page() % shown.pages() + 1);
            CommandUtil.ok(source, "page.footer", shown.page(), shown.pages(), next);
        }
        return shown.items().size();
    }

    /** Shows the MOTD to a player who just joined (when motd.showOnJoin is set). */
    public static void showMotd(ServerPlayer player) {
        TextDocument doc = TextFiles.get(player, TextFiles.MOTD);
        if (doc == null) {
            return;
        }
        for (String line : doc.chapter(null)) {
            Players.send(player, render(line, player.server, player.getGameProfile().getName()));
        }
    }

    private static Component render(String line, MinecraftServer server, String viewer) {
        String text = line
                .replace("{player}", viewer)
                .replace("{online}", String.valueOf(server.getPlayerCount()))
                .replace("{max}", String.valueOf(server.getMaxPlayers()));
        // Legacy § codes in literal text are still rendered by the client.
        return Texts.literal(COLOR_CODE.matcher(text).replaceAll("§$1"));
    }
}
