package com.mtmrails.esscmd.command;

import java.util.Locale;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mtmrails.esscmd.data.EssData;
import com.mtmrails.esscmd.data.StoredLocation;
import com.mtmrails.esscmd.permission.PermissionNodes;
import com.mtmrails.esscmd.teleport.TeleportService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

/** {@code /warp [warp] [player]}, {@code /setwarp}, {@code /delwarp}, {@code /warpinfo}. */
public final class WarpCommands {
    private static final SuggestionProvider<CommandSourceStack> WARPS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(EssData.get(ctx.getSource().getServer()).warps().keySet(), builder);

    private WarpCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("warp")
                .requires(PermissionNodes.WARP.require())
                .executes(ctx -> list(ctx.getSource()))
                .then(Commands.argument("warp", StringArgumentType.word())
                        .suggests(WARPS)
                        .executes(ctx -> {
                            StoredLocation warp = find(ctx.getSource(), StringArgumentType.getString(ctx, "warp"));
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

    private static int list(CommandSourceStack source) {
        var warps = EssData.get(source.getServer()).warps().keySet();
        if (warps.isEmpty()) {
            return CommandUtil.fail(source, "warp.none");
        }
        return CommandUtil.ok(source, "warp.list", String.join(", ", warps));
    }

    private static StoredLocation find(CommandSourceStack source, String name) {
        StoredLocation warp = EssData.get(source.getServer()).warps().get(name.toLowerCase(Locale.ROOT));
        if (warp == null) {
            CommandUtil.fail(source, "warp.not-found", name);
        }
        return warp;
    }
}
