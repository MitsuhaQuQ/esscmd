package com.mtmrails.esscmd.player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.mtmrails.esscmd.compat.Players;
import com.mtmrails.esscmd.config.ConfigManager;
import com.mtmrails.esscmd.config.EssConfig;
import com.mtmrails.esscmd.message.Broadcast;
import com.mtmrails.esscmd.message.Messages;
import com.mtmrails.esscmd.permission.PermissionNodes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * AFK state. Activity is moving or turning (polled here), chatting and running commands
 * ({@link #markActive}, called from a network-thread mixin). Players go AFK after
 * {@code afk.autoSeconds} without activity and come back on the next activity. Not persisted.
 */
public final class AfkService {
    private static final int CHECK_INTERVAL_TICKS = 20;

    private record Pose(Vec3 position, float yaw, float pitch) {
        static Pose of(ServerPlayer player) {
            return new Pose(player.position(), player.getYRot(), player.getXRot());
        }
    }

    /** Epoch millis of the last activity. Written from network threads. */
    private static final Map<UUID, Long> LAST_ACTIVE = new ConcurrentHashMap<>();
    private static final Map<UUID, Pose> LAST_POSE = new HashMap<>();
    /** AFK players -> epoch millis when they went AFK. */
    private static final Map<UUID, Long> AFK_SINCE = new HashMap<>();

    private AfkService() {}

    /** Thread-safe: only records the time. */
    public static void markActive(ServerPlayer player) {
        LAST_ACTIVE.put(player.getUUID(), System.currentTimeMillis());
    }

    public static boolean isAfk(ServerPlayer player) {
        return AFK_SINCE.containsKey(player.getUUID());
    }

    /** Server thread. {@code message} is shown in the broadcast; may be null. */
    public static void setAfk(ServerPlayer player, boolean afk, String message) {
        String name = player.getGameProfile().getName();
        if (afk) {
            AFK_SINCE.put(player.getUUID(), System.currentTimeMillis());
            if (message == null || message.isBlank()) {
                announce(player, "afk.now", name);
            } else {
                announce(player, "afk.now-message", name, message);
            }
        } else if (AFK_SINCE.remove(player.getUUID()) != null) {
            markActive(player);
            announce(player, "afk.back", name);
        }
    }

    public static void onJoin(ServerPlayer player) {
        markActive(player);
        LAST_POSE.put(player.getUUID(), Pose.of(player));
    }

    public static void onLeave(ServerPlayer player) {
        LAST_ACTIVE.remove(player.getUUID());
        LAST_POSE.remove(player.getUUID());
        AFK_SINCE.remove(player.getUUID());
    }

    public static void clear() {
        LAST_ACTIVE.clear();
        LAST_POSE.clear();
        AFK_SINCE.clear();
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % CHECK_INTERVAL_TICKS != 0) {
            return;
        }
        EssConfig.Afk config = ConfigManager.get().afk;
        long now = System.currentTimeMillis();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID id = player.getUUID();
            Pose pose = Pose.of(player);
            Pose previous = LAST_POSE.put(id, pose);
            if (previous != null && !previous.equals(pose)) {
                markActive(player);
            }
            long lastActive = LAST_ACTIVE.computeIfAbsent(id, ignored -> now);
            Long afkSince = AFK_SINCE.get(id);
            if (afkSince != null) {
                if (lastActive > afkSince) {
                    setAfk(player, false, null);
                } else if (config.kickSeconds > 0 && now - lastActive >= config.kickSeconds * 1000L
                        && !PermissionNodes.AFK_KICK_EXEMPT.check(player)) {
                    player.connection.disconnect(Messages.info(player, "afk.kicked"));
                }
            } else if (config.autoSeconds > 0 && now - lastActive >= config.autoSeconds * 1000L) {
                setAfk(player, true, null);
            }
        }
    }

    /** To everyone if {@code afk.broadcast}, else only to the player concerned. */
    private static void announce(ServerPlayer player, String key, Object... args) {
        if (ConfigManager.get().afk.broadcast) {
            Broadcast.info(player.server, key, args);
        } else {
            Players.send(player, Messages.info(player, key, args));
        }
    }
}
