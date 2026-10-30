package com.mtmrails.esscmd.player;

import java.util.Map;
import java.util.UUID;
import com.mtmrails.esscmd.command.SpawnCommands;
import com.mtmrails.esscmd.compat.Players;
import com.mtmrails.esscmd.config.ConfigManager;
import com.mtmrails.esscmd.config.EssConfig;
import com.mtmrails.esscmd.data.EssData;
import com.mtmrails.esscmd.data.PlayerRecord;
import com.mtmrails.esscmd.data.StoredLocation;
import com.mtmrails.esscmd.message.Messages;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

/**
 * Keeps jailed players inside {@code jail.radius} of their jail. Adventure mode stops them
 * breaking or placing blocks without needing block events on every loader; their own
 * gamemode comes back on release. Checked once a second, so offline expiry applies on rejoin.
 */
public final class JailService {
    private static final int CHECK_INTERVAL_TICKS = 20;

    private JailService() {}

    public static boolean isJailed(ServerPlayer player) {
        return EssData.get(player.server).player(player.getUUID()).map(r -> !r.jail().isEmpty()).orElse(false);
    }

    public static void jail(ServerPlayer player, String jail, StoredLocation location, long until) {
        EssData.get(player.server).setJailed(player.getUUID(), jail, until, StoredLocation.of(player),
                player.gameMode.getGameModeForPlayer().getName());
        move(player, location);
        player.setGameMode(GameType.ADVENTURE);
    }

    public static void release(ServerPlayer player) {
        MinecraftServer server = player.server;
        EssData data = EssData.get(server);
        PlayerRecord record = data.player(player.getUUID()).orElse(null);
        if (record == null || record.jail().isEmpty()) {
            return;
        }
        StoredLocation back = record.jailReturn();
        player.setGameMode(GameType.byName(record.jailGameMode(), GameType.SURVIVAL));
        data.clearJailed(player.getUUID());
        String mode = ConfigManager.get().jail.teleportWhenFreed;
        if (mode.equals("back") && back != null) {
            move(player, back);
        } else if (mode.equals("spawn")) {
            move(player, SpawnCommands.spawn(server));
        }
        Players.send(player, Messages.info(player, "jail.released"));
    }

    private static void move(ServerPlayer player, StoredLocation location) {
        ServerLevel level = location.level(player.server);
        if (level != null) {
            Players.teleport(player, level, location.x(), location.y(), location.z(), location.yaw(), location.pitch());
        }
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % CHECK_INTERVAL_TICKS != 0) {
            return;
        }
        EssData data = EssData.get(server);
        Map<String, StoredLocation> jails = data.jails();
        EssConfig.Jail config = ConfigManager.get().jail;
        long now = System.currentTimeMillis();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID id = player.getUUID();
            PlayerRecord record = data.player(id).orElse(null);
            if (record == null || record.jail().isEmpty()) {
                continue;
            }
            StoredLocation jail = jails.get(record.jail());
            if (jail == null || (record.jailUntil() > 0 && now >= record.jailUntil())) {
                release(player);
                continue;
            }
            StoredLocation here = StoredLocation.of(player);
            double dx = here.x() - jail.x();
            double dy = here.y() - jail.y();
            double dz = here.z() - jail.z();
            if (!here.dimension().equals(jail.dimension()) || dx * dx + dy * dy + dz * dz > config.radius * config.radius) {
                move(player, jail);
            }
            if (player.gameMode.getGameModeForPlayer() != GameType.ADVENTURE) {
                player.setGameMode(GameType.ADVENTURE);
            }
        }
    }
}
