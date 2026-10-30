package com.mtmrails.esscmd.compat;

import java.util.Optional;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import com.mtmrails.esscmd.data.EssData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
//? if <1.19 {
/*import net.minecraft.Util;
*///?}

/** Player helpers whose vanilla API differs between versions. */
public final class Players {
    private Players() {}

    public static ServerLevel level(ServerPlayer player) {
        //? if >=1.20 {
        return player.serverLevel();
        //?} else {
        /*return player.getLevel();
        *///?}
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
        player.teleportTo(level, x, y, z, yaw, pitch);
    }

    /** The client's language code, such as {@code en_us} or {@code zh_cn}. */
    public static String language(ServerPlayer player) {
        //? if >=1.20.2 {
        /*return player.clientInformation().language();
        *///?} else {
        return ((ClientLanguage) player).cmdessentials$getLanguage();
        //?}
    }

    /** True if the player has never left the server before, i.e. this is their first session. */
    public static boolean isFirstJoin(ServerPlayer player) {
        return player.getStats().getValue(Stats.CUSTOM.get(Stats.LEAVE_GAME)) == 0;
    }

    /**
     * Finds a player's UUID by name: online players first, then players the mod has stored,
     * then the server's profile cache.
     */
    public static Optional<UUID> findId(MinecraftServer server, String name) {
        ServerPlayer online = server.getPlayerList().getPlayerByName(name);
        if (online != null) {
            return Optional.of(online.getUUID());
        }
        Optional<UUID> known = EssData.get(server).findByName(name);
        if (known.isPresent()) {
            return known;
        }
        return server.getProfileCache().get(name).map(GameProfile::getId);
    }
}
