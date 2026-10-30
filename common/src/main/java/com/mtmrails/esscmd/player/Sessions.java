package com.mtmrails.esscmd.player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/** When each online player joined (epoch millis), for /seen and /whois. In memory only. */
public final class Sessions {
    private static final Map<UUID, Long> JOINED = new HashMap<>();

    private Sessions() {}

    public static void onJoin(ServerPlayer player) {
        JOINED.put(player.getUUID(), System.currentTimeMillis());
    }

    public static void onLeave(ServerPlayer player) {
        JOINED.remove(player.getUUID());
    }

    public static void clear() {
        JOINED.clear();
    }

    /** Join time, or now if unknown (e.g. right after a reload). */
    public static long joinedAt(ServerPlayer player) {
        return JOINED.getOrDefault(player.getUUID(), System.currentTimeMillis());
    }
}
