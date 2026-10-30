package com.mtmrails.esscmd.command;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mtmrails.esscmd.config.ConfigManager;
import com.mtmrails.esscmd.data.EssData;
import com.mtmrails.esscmd.data.StoredLocation;
import com.mtmrails.esscmd.permission.PermissionNodes;
import com.mtmrails.esscmd.platform.Platform;
import com.mtmrails.esscmd.teleport.TeleportService;
import com.mtmrails.esscmd.util.Pager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

/** {@code /warp [warp] [player]}, {@code /setwarp}, {@code /delwarp}, {@code /warpinfo}. */
public final class WarpCommands {
    private static final int WARPS_PER_PAGE = 40;
    // Up to 4 digits: a longer number is more likely a warp name than a page.
    private static final Pattern PAGE_NUMBER = Pattern.compile("[1-9][0-9]{0,3}");
    private static final SuggestionProvider<CommandSourceStack> WARPS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(EssData.get(ctx.getSource().getServer()).warps().keySet(), builder);

    private WarpCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("warp")
                .requires(PermissionNodes.WARP.require())
                .executes(ctx -> list(ctx.getSource(), 1))
                .then(Commands.argument("warp", StringArgumentType.word())
                        .suggests(WARPS)
                        .executes(ctx -> {
                            String arg = StringArgumentType.getString(ctx, "warp");
                            if (PAGE_NUMBER.matcher(arg).matches()) {
                                return list(ctx.getSource(), Integer.parseInt(arg));
                            }
                            StoredLocation warp = find(ctx.getSource(), arg);
                            if (warp == null) {
                                return 0;
                            }
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            return TeleportService.teleport(player, () -> warp) ? 1 : 0;
                        })
                        .then(Commands.argument("player", EntityArgument.player())
                                .requires(PermissionNodes.WARP_OTHERS.require())
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "warp");
                                    StoredLocation warp = find(ctx.getSource(), name);
                                    if (warp == null) {
                                        return 0;
                                    }
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    if (!TeleportService.teleportNow(target, warp)) {
                                        return 0;
                                    }
                                    return CommandUtil.ok(ctx.getSource(), "warp.sent", target.getGameProfile().getName(),
                                            name.toLowerCase(Locale.ROOT));
                                }))));
        dispatcher.register(Commands.literal("setwarp")
                .requires(PermissionNodes.SETWARP.require())
                .then(Commands.argument("warp", StringArgumentType.word())
                        .executes(ctx -> {
                            String name = CommandUtil.validName(ctx.getSource(), StringArgumentType.getString(ctx, "warp"));
                            if (name == null) {
                                return 0;
                            }
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            EssData.get(ctx.getSource().getServer()).setWarp(name, StoredLocation.of(player));
                            return CommandUtil.ok(ctx.getSource(), "warp.set", name);
                        })));
        dispatcher.register(Commands.literal("delwarp")
                .requires(PermissionNodes.DELWARP.require())
                .then(Commands.argument("warp", StringArgumentType.word())
                        .suggests(WARPS)
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "warp").toLowerCase(Locale.ROOT);
                            if (!EssData.get(ctx.getSource().getServer()).removeWarp(name)) {
                                return CommandUtil.fail(ctx.getSource(), "warp.not-found", name);
                            }
                            return CommandUtil.ok(ctx.getSource(), "warp.deleted", name);
                        })));
        dispatcher.register(Commands.literal("warpinfo")
                .requires(PermissionNodes.WARPINFO.require())
                .then(Commands.argument("warp", StringArgumentType.word())
                        .suggests(WARPS)
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "warp").toLowerCase(Locale.ROOT);
                            StoredLocation warp = find(ctx.getSource(), name);
                            return warp == null ? 0 : CommandUtil.ok(ctx.getSource(), "warp.info", name, warp.describe());
                        })));
    }

    /** warps.perWarpPermission: needs cmdessentials.warps.<name>; admins pass by OP level. */
    private static boolean allowed(CommandSourceStack source, String warp) {
        return !ConfigManager.get().warps.perWarpPermission
                || Platform.get().hasPermission(source, "cmdessentials.warps." + warp, 2);
    }

    /** {@code /warp <page>}: Essentials accepts a page number in place of a warp name. */
    private static int list(CommandSourceStack source, int page) {
        List<String> warps = EssData.get(source.getServer()).warps().keySet().stream()
                .filter(name -> allowed(source, name)).toList();
        if (warps.isEmpty()) {
            return CommandUtil.fail(source, "warp.none");
        }
        Pager.Page<String> shown = Pager.page(warps, page, WARPS_PER_PAGE);
        CommandUtil.ok(source, "warp.list", String.join(", ", shown.items()));
        if (shown.pages() > 1) {
            CommandUtil.ok(source, "page.footer", shown.page(), shown.pages(), "/warp " + (shown.page() % shown.pages() + 1));
        }
        return shown.items().size();
    }

    private static StoredLocation find(CommandSourceStack source, String name) {
        String key = name.toLowerCase(Locale.ROOT);
        StoredLocation warp = EssData.get(source.getServer()).warps().get(key);
        if (warp == null) {
            CommandUtil.fail(source, "warp.not-found", name);
        } else if (!allowed(source, key)) {
            CommandUtil.fail(source, "warp.no-access", key);
            return null;
        }
        return warp;
    }
}
