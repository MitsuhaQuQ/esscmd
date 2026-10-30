package com.mtmrails.esscmd.message;

import com.mtmrails.esscmd.compat.Players;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Sends one message key to everyone, each player in their own language, and logs it to the console. */
public final class Broadcast {
    private Broadcast() {}

    public static void info(MinecraftServer server, String key, Object... args) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Players.send(player, Messages.info(player, key, args));
        }
        Players.sendToConsole(server, Messages.info(server.createCommandSourceStack(), key, args));
    }
}
