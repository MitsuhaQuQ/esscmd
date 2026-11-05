package com.mtmrails.esscmd.permission;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Whether other players can be on this server: always on a dedicated server, and on a client's
 * integrated server only once it is opened to LAN. Chat commands need it.
 */
public final class Multiplayer {
    private static boolean wasActive;

    private Multiplayer() {}

    public static boolean active(MinecraftServer server) {
        return server == null || server.isDedicatedServer() || server.isPublished();
    }

    /** Re-sends the command tree when a world is opened to LAN, so the chat commands appear. */
    public static void tick(MinecraftServer server) {
        boolean now = active(server);
        if (now != wasActive) {
            wasActive = now;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                server.getCommands().sendCommands(player);
            }
        }
    }

    public static void clear() {
        wasActive = false;
    }
}
