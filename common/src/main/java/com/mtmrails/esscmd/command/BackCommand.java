package com.mtmrails.esscmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mtmrails.esscmd.data.EssData;
import com.mtmrails.esscmd.data.StoredLocation;
import com.mtmrails.esscmd.permission.PermissionNodes;
import com.mtmrails.esscmd.teleport.TeleportService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

/** {@code /back [player]}: the location before the last teleport, or the last death if enabled. */
public final class BackCommand {
    private BackCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("back")
                .requires(PermissionNodes.BACK.require())
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    StoredLocation back = EssData.get(player.server).back(player.getUUID());
                    if (back == null) {
                        return CommandUtil.fail(ctx.getSource(), "back.none");
                    }
                    return TeleportService.teleport(player, () -> back) ? 1 : 0;
                })
                .then(Commands.argument("player", EntityArgument.player())
                        .requires(PermissionNodes.BACK_OTHERS.require())
                        .executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                            String name = target.getGameProfile().getName();
                            StoredLocation back = EssData.get(target.server).back(target.getUUID());
                            if (back == null) {
                                return CommandUtil.fail(ctx.getSource(), "back.none-other", name);
                            }
                            if (!TeleportService.teleportNow(target, back)) {
                                return 0;
                            }
                            return CommandUtil.ok(ctx.getSource(), "back.sent", name);
                        })));
    }
}
