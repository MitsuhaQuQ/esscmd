package com.mtmrails.esscmd.command;

import java.util.Comparator;
import java.util.List;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mtmrails.esscmd.compat.Players;
import com.mtmrails.esscmd.config.ConfigManager;
import com.mtmrails.esscmd.data.StoredLocation;
import com.mtmrails.esscmd.permission.PermissionNodes;
import com.mtmrails.esscmd.player.VanishService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

/** {@code /getpos [player]}, {@code /compass}, {@code /depth}, {@code /near [player] [radius]}. */
public final class LocationInfoCommands {
    /** Eight-point compass, starting at north and going clockwise. */
    private static final String[] BEARINGS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

    private LocationInfoCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("getpos")
                .requires(PermissionNodes.GETPOS.require())
                .executes(ctx -> getPos(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                        .requires(PermissionNodes.GETPOS_OTHERS.require())
                        .executes(ctx -> getPos(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))));
        dispatcher.register(Commands.literal("compass")
                .requires(PermissionNodes.COMPASS.require())
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    int degrees = bearingDegrees(player.getYRot());
                    return CommandUtil.ok(ctx.getSource(), "compass.bearing", bearing(degrees), degrees);
                }));
        dispatcher.register(Commands.literal("depth")
                .requires(PermissionNodes.DEPTH.require())
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    int depth = Mth.floor(player.getY()) - Players.level(player).getSeaLevel();
                    if (depth > 0) {
                        return CommandUtil.ok(ctx.getSource(), "depth.above", depth);
                    }
                    return depth < 0 ? CommandUtil.ok(ctx.getSource(), "depth.below", -depth)
                            : CommandUtil.ok(ctx.getSource(), "depth.at");
                }));
        dispatcher.register(Commands.literal("near")
                .requires(PermissionNodes.NEAR.require())
                .executes(ctx -> near(ctx.getSource(), ctx.getSource().getPlayerOrException(), defaultRadius()))
                .then(Commands.argument("radius", IntegerArgumentType.integer(1))
                        .executes(ctx -> near(ctx.getSource(), ctx.getSource().getPlayerOrException(),
                                IntegerArgumentType.getInteger(ctx, "radius"))))
                .then(Commands.argument("player", EntityArgument.player())
                        .requires(PermissionNodes.NEAR_OTHERS.require())
                        .executes(ctx -> near(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), defaultRadius()))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1))
                                .executes(ctx -> near(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"),
                                        IntegerArgumentType.getInteger(ctx, "radius"))))));
    }

    private static int getPos(CommandSourceStack source, ServerPlayer player) {
        StoredLocation location = StoredLocation.of(player);
        return CommandUtil.ok(source, "getpos.position", player.getGameProfile().getName(), location.describe(),
                Math.round(Mth.wrapDegrees(player.getYRot())), Math.round(player.getXRot()));
    }

    /** Minecraft yaw: 0 = south, 90 = west, 180 = north. Returns 0-359 with 0 = north, clockwise. */
    static int bearingDegrees(float yaw) {
        return Math.floorMod(Math.round(yaw) + 180, 360);
    }

    static String bearing(int degrees) {
        return BEARINGS[Math.floorMod(Math.round(degrees / 45f), 8)];
    }

    private static int defaultRadius() {
        return ConfigManager.get().near.defaultRadius;
    }

    private static int near(CommandSourceStack source, ServerPlayer center, int radius) {
        int capped = Math.min(radius, ConfigManager.get().near.maxRadius);
        double maxSqr = (double) capped * capped;
        List<ServerPlayer> nearby = Players.level(center).players().stream()
                .filter(other -> other != center && other.distanceToSqr(center) <= maxSqr)
                .filter(other -> !(source.getEntity() instanceof ServerPlayer viewer && VanishService.hiddenFrom(other, viewer)))
                .sorted(Comparator.comparingDouble(other -> other.distanceToSqr(center)))
                .toList();
        if (nearby.isEmpty()) {
            return CommandUtil.ok(source, "near.none", capped);
        }
        String list = String.join(", ", nearby.stream()
                .map(other -> other.getGameProfile().getName() + " (" + Math.round(Math.sqrt(other.distanceToSqr(center))) + "m)")
                .toList());
        return CommandUtil.ok(source, "near.list", capped, list) * nearby.size();
    }
}
