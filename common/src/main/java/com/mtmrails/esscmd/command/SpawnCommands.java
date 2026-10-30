package com.mtmrails.esscmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mtmrails.esscmd.data.EssData;
import com.mtmrails.esscmd.data.StoredLocation;
import com.mtmrails.esscmd.permission.PermissionNodes;
import com.mtmrails.esscmd.teleport.TeleportService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** {@code /setspawn} and {@code /spawn [player]}. Falls back to the world spawn until /setspawn is used. */
public final class SpawnCommands {
    private SpawnCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("setspawn")
                .requires(PermissionNodes.SETSPAWN.require())
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    StoredLocation location = StoredLocation.of(player);
                    EssData.get(ctx.getSource().getServer()).setSpawn(location);
                    return CommandUtil.ok(ctx.getSource(), "spawn.set", location.describe());
                }));
        dispatcher.register(Commands.literal("spawn")
                .requires(PermissionNodes.SPAWN.require())
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    return TeleportService.teleport(player, () -> spawn(player.server)) ? 1 : 0;
                })
                .then(Commands.argument("player", EntityArgument.player())
                        .requires(PermissionNodes.SPAWN_OTHERS.require())
                        .executes(ctx -> spawnOther(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))));
    }

    public static StoredLocation spawn(MinecraftServer server) {
        StoredLocation spawn = EssData.get(server).spawn();
        return spawn != null ? spawn : StoredLocation.worldSpawn(server);
    }

    private static int spawnOther(CommandSourceStack source, ServerPlayer target) throws CommandSyntaxException {
        if (!TeleportService.teleportNow(target, spawn(source.getServer()))) {
            return 0;
        }
        return CommandUtil.ok(source, "spawn.sent", target.getGameProfile().getName());
    }
}
