package com.mtmrails.esscmd.compat;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.mtmrails.esscmd.data.EssData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.world.item.ItemStack;
//? if >=26.1 {
/*import java.util.Map;
import java.util.Set;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.Prediction;
import net.minecraft.world.clock.ClockNetworkState;
*///?} else {
import com.mojang.authlib.GameProfile;
import net.minecraft.world.level.GameRules;
//?}
//? if <1.19 {
/*import net.minecraft.Util;
*///?}

/** Player helpers whose vanilla API differs between versions. */
public final class Players {
    private Players() {}

    public static String name(ServerPlayer player) {
        //? if >=1.21.9 {
        /*return player.getGameProfile().name();
        *///?} else {
        return player.getGameProfile().getName();
        //?}
    }

    public static MinecraftServer server(ServerPlayer player) {
        //? if >=1.21.6 {
        /*return player.level().getServer();
        *///?} else {
        return player.server;
        //?}
    }

    public static ServerLevel level(ServerPlayer player) {
        //? if >=1.21.6 {
        /*return player.level();
        *///?} else if >=1.20 {
        return player.serverLevel();
        //?} else {
        /*return player.getLevel();
        *///?}
    }

    public static boolean hasOpLevel(CommandSourceStack source, int level) {
        //? if >=26.1 {
        /*return source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(level)));
        *///?} else {
        return source.hasPermission(level);
        //?}
    }

    public static boolean hasOpLevel(ServerPlayer player, int level) {
        //? if >=26.1 {
        /*return player.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(level)));
        *///?} else {
        return player.hasPermissions(level);
        //?}
    }

    /** Whether the player has a bed or respawn anchor set. */
    public static boolean hasRespawnPoint(ServerPlayer player) {
        //? if >=26.1 {
        /*return player.getRespawnConfig() != null;
        *///?} else {
        return player.getRespawnPosition() != null;
        //?}
    }

    public static void kill(ServerPlayer player) {
        //? if >=26.1 {
        /*player.kill(level(player));
        *///?} else {
        player.kill();
        //?}
    }

    /** Drops an item in front of the player. */
    public static void drop(ServerPlayer player, ItemStack stack) {
        //? if >=26.1 {
        /*player.drop(stack, false, Prediction.SERVER_ONLY);
        *///?} else {
        player.drop(stack, false);
        //?}
    }

    /** The 36 slots of the main inventory, without armor and offhand. */
    public static List<ItemStack> mainItems(ServerPlayer player) {
        //? if >=26.1 {
        /*return player.getInventory().getNonEquipmentItems();
        *///?} else {
        return player.getInventory().items;
        //?}
    }

    public static boolean isOp(MinecraftServer server, ServerPlayer player) {
        //? if >=26.1 {
        /*return server.getPlayerList().isOp(new NameAndId(player.getGameProfile()));
        *///?} else {
        return server.getPlayerList().isOp(player.getGameProfile());
        //?}
    }

    public static void ban(MinecraftServer server, UUID id, String name, Date from, String source, Date until, String reason) {
        //? if >=26.1 {
        /*server.getPlayerList().getBans().add(new UserBanListEntry(new NameAndId(id, name), from, source, until, reason));
        *///?} else {
        server.getPlayerList().getBans().add(new UserBanListEntry(new GameProfile(id, name), from, source, until, reason));
        //?}
    }

    public static boolean isBanned(MinecraftServer server, UUID id, String name) {
        //? if >=26.1 {
        /*return server.getPlayerList().getBans().isBanned(new NameAndId(id, name));
        *///?} else {
        return server.getPlayerList().getBans().isBanned(new GameProfile(id, name));
        //?}
    }

    public static void unban(MinecraftServer server, UUID id, String name) {
        //? if >=26.1 {
        /*server.getPlayerList().getBans().remove(new NameAndId(id, name));
        *///?} else {
        server.getPlayerList().getBans().remove(new GameProfile(id, name));
        //?}
    }

    /** Shows {@code ticks} as the frozen time of day on this client only. */
    public static void sendTime(ServerPlayer player, long ticks) {
        ServerLevel level = level(player);
        //? if >=26.1 {
        /*level.dimensionType().defaultClock().ifPresent(clock -> player.connection.send(new ClientboundSetTimePacket(
                level.getGameTime(), Map.of(clock, new ClockNetworkState(ticks, 0.0F, 0.0F)))));
        *///?} else {
        player.connection.send(new ClientboundSetTimePacket(level.getGameTime(), ticks, false));
        //?}
    }

    /** Puts this client back on the real time of day. */
    public static void resetTime(ServerPlayer player) {
        //? if >=26.1 {
        /*player.connection.send(server(player).clockManager().createFullSyncPacket());
        *///?} else {
        ServerLevel level = level(player);
        player.connection.send(new ClientboundSetTimePacket(level.getGameTime(), level.getDayTime(),
                level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)));
        //?}
    }

    public static void send(ServerPlayer player, Component message) {
        //? if >=1.19 {
        player.sendSystemMessage(message);
        //?} else {
        /*player.sendMessage(message, Util.NIL_UUID);
        *///?}
    }

    public static void sendToConsole(MinecraftServer server, Component message) {
        //? if >=1.19 {
        server.sendSystemMessage(message);
        //?} else {
        /*server.sendMessage(message, Util.NIL_UUID);
        *///?}
    }

    public static void teleport(ServerPlayer player, ServerLevel level, double x, double y, double z, float yaw, float pitch) {
        //? if >=26.1 {
        /*player.teleportTo(level, x, y, z, Set.of(), yaw, pitch, true);
        *///?} else {
        player.teleportTo(level, x, y, z, yaw, pitch);
        //?}
    }

    /** The client's language code, such as {@code en_us} or {@code zh_cn}. */
    public static String language(ServerPlayer player) {
        //? if >=1.20.2 {
        /*return player.clientInformation().language();
        *///?} else {
        return ((ClientLanguage) player).cmdessentials$getLanguage();
        //?}
    }

    /** Round-trip latency in milliseconds, as shown in the tab list. */
    public static int latency(ServerPlayer player) {
        //? if >=1.20.2 {
        /*return player.connection.latency();
        *///?} else {
        return player.latency;
        //?}
    }

    /** True if the player has never left the server before, i.e. this is their first session. */
    public static boolean isFirstJoin(ServerPlayer player) {
        return player.getStats().getValue(Stats.CUSTOM.get(Stats.LEAVE_GAME)) == 0;
    }

    /**
     * Finds a player's UUID by name: online players first, then players the mod has recorded
     * (names are stored on join). Deliberately not the vanilla profile cache: for unknown names
     * it asks Mojang's API, blocking the server thread.
     */
    public static Optional<UUID> findId(MinecraftServer server, String name) {
        ServerPlayer online = server.getPlayerList().getPlayerByName(name);
        if (online != null) {
            return Optional.of(online.getUUID());
        }
        return EssData.get(server).findByName(name);
    }
}
