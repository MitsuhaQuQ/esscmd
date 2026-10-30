package com.mtmrails.esscmd.command;

import java.util.Optional;
import java.util.UUID;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mtmrails.esscmd.CmdEssentials;
import com.mtmrails.esscmd.compat.Dimensions;
import com.mtmrails.esscmd.compat.Players;
import com.mtmrails.esscmd.config.ConfigManager;
import com.mtmrails.esscmd.config.EssConfig;
import com.mtmrails.esscmd.data.EssData;
import com.mtmrails.esscmd.data.PlayerRecord;
import com.mtmrails.esscmd.data.StoredLocation;
import com.mtmrails.esscmd.permission.PermissionNodes;
import com.mtmrails.esscmd.teleport.RandomTeleport;
import com.mtmrails.esscmd.teleport.SafeSpots;
import com.mtmrails.esscmd.teleport.TeleportService;
import com.mtmrails.esscmd.teleport.TpaRequests;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * P1 M4 teleports: {@code /tphere}, {@code /tppos}, {@code /tpo}, {@code /tpohere},
 * {@code /tpall}, {@code /tpaall}, {@code /tpoffline}, {@code /top}, {@code /bottom},
 * {@code /jump}, {@code /tpr}, {@code /settpr}, {@code /world}.
 * A player's own moves go through {@link TeleportService#teleport} (delay, cooldown); moving
 * someone else uses {@link TeleportService#teleportNow}.
 */
public final class TeleportExtraCommands {
    private static final int JUMP_DISTANCE = 120;
    private static final int JUMP_SEARCH_UP = 16;

    private TeleportExtraCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tphere")
                .requires(PermissionNodes.TPHERE.require())
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> bringHere(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), false))));
        dispatcher.register(Commands.literal("tpohere")
                .requires(PermissionNodes.TPOHERE.require())
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> bringHere(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), true))));
        dispatcher.register(Commands.literal("tpo")
                .requires(PermissionNodes.TPO.require())
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> moveTo(ctx.getSource(), ctx.getSource().getPlayerOrException(),
                                EntityArgument.getPlayer(ctx, "player")))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> moveTo(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"),
                                        EntityArgument.getPlayer(ctx, "target"))))));
        dispatcher.register(Commands.literal("tppos")
                .requires(PermissionNodes.TPPOS.require())
                .then(tpposArguments()));
        dispatcher.register(Commands.literal("tpall")
                .requires(PermissionNodes.TPALL.require())
                .executes(ctx -> tpAll(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> tpAll(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))));
        dispatcher.register(Commands.literal("tpaall")
                .requires(PermissionNodes.TPAALL.require())
                .executes(ctx -> tpaAll(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> tpaAll(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))));
        dispatcher.register(Commands.literal("tpoffline")
                .requires(PermissionNodes.TPOFFLINE.require())
                .then(Commands.argument("player", StringArgumentType.word())
                        .executes(ctx -> tpOffline(ctx.getSource(), StringArgumentType.getString(ctx, "player")))));
        dispatcher.register(Commands.literal("top")
                .requires(PermissionNodes.TOP.require())
                .executes(ctx -> top(ctx.getSource().getPlayerOrException(), ctx.getSource())));
        dispatcher.register(Commands.literal("bottom")
                .requires(PermissionNodes.BOTTOM.require())
                .executes(ctx -> bottom(ctx.getSource().getPlayerOrException(), ctx.getSource())));
        dispatcher.register(Commands.literal("jump")
                .requires(PermissionNodes.JUMP.require())
                .executes(ctx -> jump(ctx.getSource().getPlayerOrException(), ctx.getSource())));
        dispatcher.register(Commands.literal("tpr")
                .requires(PermissionNodes.TPR.require())
                .executes(ctx -> randomTeleport(ctx.getSource().getPlayerOrException(), ctx.getSource())));
        dispatcher.register(Commands.literal("settpr")
                .requires(PermissionNodes.SETTPR.require())
                .then(Commands.argument("world", DimensionArgument.dimension())
                        .executes(ctx -> showTpr(ctx.getSource(), DimensionArgument.getDimension(ctx, "world")))
                        .then(Commands.literal("center")
                                .executes(ctx -> setTprCenter(ctx.getSource(), DimensionArgument.getDimension(ctx, "world"))))
                        .then(Commands.literal("minrange")
                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                        .executes(ctx -> setTprRange(ctx, false))))
                        .then(Commands.literal("maxrange")
                                .then(Commands.argument("value", IntegerArgumentType.integer(1))
                                        .executes(ctx -> setTprRange(ctx, true))))));
        dispatcher.register(Commands.literal("world")
                .requires(PermissionNodes.WORLD.require())
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    MinecraftServer server = player.server;
                    // Like Essentials: without a world, switch between the overworld and the Nether.
                    boolean inOverworld = Players.level(player).dimension() == Level.OVERWORLD;
                    ServerLevel target = server.getLevel(inOverworld ? Level.NETHER : Level.OVERWORLD);
                    return target == null ? CommandUtil.fail(ctx.getSource(), "teleport.unknown-world", "minecraft:the_nether")
                            : world(ctx.getSource(), player, target);
                })
                .then(Commands.argument("world", DimensionArgument.dimension())
                        .executes(ctx -> world(ctx.getSource(), ctx.getSource().getPlayerOrException(),
                                DimensionArgument.getDimension(ctx, "world")))));
    }

    private static String name(ServerPlayer player) {
        return player.getGameProfile().getName();
    }

    private static int bringHere(CommandSourceStack source, ServerPlayer target, boolean override) throws CommandSyntaxException {
        ServerPlayer self = source.getPlayerOrException();
        if (!override && EssData.get(source.getServer()).teleportDisabled(target.getUUID())) {
            return CommandUtil.fail(source, "tpa.disabled", name(target));
        }
        if (!TeleportService.teleportNow(target, StoredLocation.of(self))) {
            return 0;
        }
        return CommandUtil.ok(source, "tphere.done", name(target));
    }

    /** /tpo: ignores /tptoggle. Moving yourself is still instant: /tpo is an admin override. */
    private static int moveTo(CommandSourceStack source, ServerPlayer who, ServerPlayer destination) {
        if (!TeleportService.teleportNow(who, StoredLocation.of(destination))) {
            return 0;
        }
        return CommandUtil.ok(source, "tpo.done", name(who), name(destination));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, ?> tpposArguments() {
        return Commands.argument("pos", Vec3Argument.vec3())
                .executes(ctx -> tppos(ctx, false, false))
                .then(Commands.argument("yaw", FloatArgumentType.floatArg(-180, 180))
                        .then(Commands.argument("pitch", FloatArgumentType.floatArg(-90, 90))
                                .executes(ctx -> tppos(ctx, true, false))
                                .then(Commands.argument("world", DimensionArgument.dimension())
                                        .executes(ctx -> tppos(ctx, true, true)))));
    }

    private static int tppos(CommandContext<CommandSourceStack> ctx, boolean rotation, boolean world) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Vec3 pos = Vec3Argument.getVec3(ctx, "pos");
        ServerLevel level = world ? DimensionArgument.getDimension(ctx, "world") : Players.level(player);
        float yaw = rotation ? FloatArgumentType.getFloat(ctx, "yaw") : player.getYRot();
        float pitch = rotation ? FloatArgumentType.getFloat(ctx, "pitch") : player.getXRot();
        StoredLocation target = new StoredLocation(Dimensions.id(level.dimension()), pos.x, pos.y, pos.z, yaw, pitch);
        return TeleportService.teleport(player, () -> target) ? 1 : 0;
    }

    private static int tpAll(CommandSourceStack source, ServerPlayer destination) {
        StoredLocation where = StoredLocation.of(destination);
        int moved = 0;
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            if (player != destination && TeleportService.teleportNow(player, where)) {
                moved++;
            }
        }
        return CommandUtil.ok(source, "tpall.done", moved, name(destination)) * Math.max(moved, 1);
    }

    /** Sends a /tpahere request from {@code requester} to everyone else who accepts requests. */
    private static int tpaAll(CommandSourceStack source, ServerPlayer requester) {
        MinecraftServer server = source.getServer();
        EssData data = EssData.get(server);
        int timeout = ConfigManager.get().tpa.timeoutSeconds;
        int sent = 0;
        for (ServerPlayer target : server.getPlayerList().getPlayers()) {
            if (target == requester || data.teleportDisabled(target.getUUID())
                    || data.ignores(target.getUUID(), requester.getUUID())) {
                continue;
            }
            TpaRequests.add(new TpaRequests.Request(requester.getUUID(), name(requester), target.getUUID(), name(target),
                    TpaRequests.Type.HERE, server.getTickCount() + timeout * 20));
            TpaCommands.notifyTarget(target, name(requester), TpaRequests.Type.HERE, timeout);
            sent++;
        }
        return CommandUtil.ok(source, "tpaall.sent", sent) * Math.max(sent, 1);
    }

    private static int tpOffline(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer self = source.getPlayerOrException();
        MinecraftServer server = source.getServer();
        Optional<UUID> id = Players.findId(server, name);
        StoredLocation logout = id.flatMap(uuid -> EssData.get(server).player(uuid)).map(PlayerRecord::logoutLocation).orElse(null);
        if (logout == null) {
            return CommandUtil.fail(source, "tpoffline.none", name);
        }
        return TeleportService.teleportNow(self, logout) ? CommandUtil.ok(source, "tpoffline.done", name) : 0;
    }

    private static int top(ServerPlayer player, CommandSourceStack source) {
        ServerLevel level = Players.level(player);
        BlockPos column = player.blockPosition();
        Optional<BlockPos> spot = SafeSpots.inColumn(level, column.getX(), column.getZ());
        if (spot.isEmpty()) {
            return CommandUtil.fail(source, "teleport.no-safe-spot");
        }
        StoredLocation target = SafeSpots.location(level, spot.get(), player.getYRot(), player.getXRot());
        return TeleportService.teleport(player, () -> target) ? 1 : 0;
    }

    private static int bottom(ServerPlayer player, CommandSourceStack source) {
        ServerLevel level = Players.level(player);
        BlockPos column = player.blockPosition();
        Optional<BlockPos> spot = SafeSpots.lowestInColumn(level, column.getX(), column.getZ(), column.getY());
        if (spot.isEmpty()) {
            return CommandUtil.fail(source, "teleport.no-safe-spot");
        }
        StoredLocation target = SafeSpots.location(level, spot.get(), player.getYRot(), player.getXRot());
        return TeleportService.teleport(player, () -> target) ? 1 : 0;
    }

    private static int jump(ServerPlayer player, CommandSourceStack source) {
        HitResult hit = player.pick(JUMP_DISTANCE, 1.0f, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return CommandUtil.fail(source, "jump.nothing");
        }
        ServerLevel level = Players.level(player);
        BlockPos block = ((BlockHitResult) hit).getBlockPos();
        Optional<BlockPos> spot = SafeSpots.above(level, block.above(), JUMP_SEARCH_UP);
        if (spot.isEmpty()) {
            return CommandUtil.fail(source, "teleport.no-safe-spot");
        }
        StoredLocation target = SafeSpots.location(level, spot.get(), player.getYRot(), player.getXRot());
        return TeleportService.teleport(player, () -> target) ? 1 : 0;
    }

    private static int randomTeleport(ServerPlayer player, CommandSourceStack source) {
        ServerLevel level = Players.level(player);
        EssConfig.TprArea area = RandomTeleport.area(level);
        if (area == null) {
            return CommandUtil.fail(source, "tpr.disabled", Dimensions.id(level.dimension()));
        }
        Optional<StoredLocation> spot = RandomTeleport.find(level, area);
        if (spot.isEmpty()) {
            return CommandUtil.fail(source, "tpr.failed");
        }
        StoredLocation target = spot.get();
        return TeleportService.teleport(player, () -> target) ? 1 : 0;
    }

    private static int showTpr(CommandSourceStack source, ServerLevel level) {
        EssConfig.TprArea area = RandomTeleport.area(level);
        String dimension = Dimensions.id(level.dimension());
        if (area == null) {
            return CommandUtil.fail(source, "tpr.disabled", dimension);
        }
        return CommandUtil.ok(source, "settpr.show", dimension, area.centerX + ", " + area.centerZ, area.minRange, area.maxRange);
    }

    private static EssConfig.TprArea areaFor(ServerLevel level) {
        return ConfigManager.get().tpr.areas.computeIfAbsent(Dimensions.id(level.dimension()),
                id -> new EssConfig.TprArea(0, 0, 0, 1000));
    }

    private static int setTprCenter(CommandSourceStack source, ServerLevel level) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        EssConfig.TprArea area = areaFor(level);
        area.centerX = Mth.floor(player.getX());
        area.centerZ = Mth.floor(player.getZ());
        return saved(source, level);
    }

    private static int setTprRange(CommandContext<CommandSourceStack> ctx, boolean max) throws CommandSyntaxException {
        ServerLevel level = DimensionArgument.getDimension(ctx, "world");
        int value = IntegerArgumentType.getInteger(ctx, "value");
        EssConfig.TprArea area = areaFor(level);
        int min = max ? area.minRange : value;
        int maxRange = max ? value : area.maxRange;
        // Validate before changing anything, so a rejected value never reaches /tpr.
        if (min >= maxRange) {
            return CommandUtil.fail(ctx.getSource(), "settpr.invalid-range", min, maxRange);
        }
        area.minRange = min;
        area.maxRange = maxRange;
        return saved(ctx.getSource(), level);
    }

    private static int saved(CommandSourceStack source, ServerLevel level) {
        ConfigManager.save().whenComplete((ignored, error) -> {
            if (error != null) {
                CmdEssentials.LOGGER.error("Could not save the cmdEssentials config", error);
            }
        });
        return showTpr(source, level);
    }

    /**
     * The End's spawn platform only exists after someone used a portal, so fall back to the
     * main island (searched outwards from the exit portal along +x).
     */
    private static Optional<BlockPos> endSpot(ServerLevel end) {
        if (SafeSpots.isSafe(end, ServerLevel.END_SPAWN_POINT)) {
            return Optional.of(ServerLevel.END_SPAWN_POINT);
        }
        for (int x = 10; x <= 80; x += 5) {
            Optional<BlockPos> spot = SafeSpots.inColumn(end, x, 0);
            if (spot.isPresent()) {
                return spot;
            }
        }
        return Optional.empty();
    }

    /**
     * /world: the overworld goes to the world spawn, the End to its spawn platform, other
     * dimensions to the scaled position of the player (Nether: x/8) at a safe height.
     */
    private static int world(CommandSourceStack source, ServerPlayer player, ServerLevel target) {
        StoredLocation destination;
        if (target.dimension() == Level.OVERWORLD) {
            destination = StoredLocation.worldSpawn(source.getServer());
        } else if (target.dimension() == Level.END) {
            Optional<BlockPos> spot = endSpot(target);
            if (spot.isEmpty()) {
                return CommandUtil.fail(source, "teleport.no-safe-spot");
            }
            destination = SafeSpots.location(target, spot.get(), player.getYRot(), 0);
        } else {
            double scale = Players.level(player).dimensionType().coordinateScale() / target.dimensionType().coordinateScale();
            int x = Mth.floor(player.getX() * scale);
            int z = Mth.floor(player.getZ() * scale);
            Optional<BlockPos> spot = SafeSpots.inColumn(target, x, z);
            if (spot.isEmpty()) {
                return CommandUtil.fail(source, "teleport.no-safe-spot");
            }
            destination = SafeSpots.location(target, spot.get(), player.getYRot(), player.getXRot());
        }
        StoredLocation finalDestination = destination;
        return TeleportService.teleport(player, () -> finalDestination) ? 1 : 0;
    }
}
