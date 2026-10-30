package com.mtmrails.esscmd.player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.mtmrails.esscmd.compat.Players;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;

/**
 * /ptime and /pweather: what one client is told, without touching the world. Vanilla broadcasts
 * the time every 20 ticks and our tick runs after it, so re-sending keeps the override.
 * Not persisted.
 */
public final class PlayerView {
    private static final int RESEND_TICKS = 20;

    private static final Map<UUID, Long> TIME = new HashMap<>();
    /** true = storm, false = clear. */
    private static final Map<UUID, Boolean> WEATHER = new HashMap<>();

    private PlayerView() {}

    public static void setTime(ServerPlayer player, long ticks) {
        TIME.put(player.getUUID(), ticks);
        sendTime(player, ticks);
    }

    public static void resetTime(ServerPlayer player) {
        TIME.remove(player.getUUID());
        var level = Players.level(player);
        player.connection.send(new ClientboundSetTimePacket(level.getGameTime(), level.getDayTime(),
                level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)));
    }

    public static void setWeather(ServerPlayer player, boolean storm) {
        WEATHER.put(player.getUUID(), storm);
        sendWeather(player, storm);
    }

    public static void resetWeather(ServerPlayer player) {
        WEATHER.remove(player.getUUID());
        sendWeather(player, Players.level(player).isRaining());
    }

    public static Long time(ServerPlayer player) {
        return TIME.get(player.getUUID());
    }

    public static Boolean weather(ServerPlayer player) {
        return WEATHER.get(player.getUUID());
    }

    private static void sendTime(ServerPlayer player, long ticks) {
        player.connection.send(new ClientboundSetTimePacket(Players.level(player).getGameTime(), ticks, false));
    }

    private static void sendWeather(ServerPlayer player, boolean storm) {
        player.connection.send(new ClientboundGameEventPacket(
                storm ? ClientboundGameEventPacket.START_RAINING : ClientboundGameEventPacket.STOP_RAINING, 0));
        player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, storm ? 1 : 0));
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % RESEND_TICKS != 0 || (TIME.isEmpty() && WEATHER.isEmpty())) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Long time = TIME.get(player.getUUID());
            if (time != null) {
                sendTime(player, time);
            }
            Boolean storm = WEATHER.get(player.getUUID());
            if (storm != null) {
                sendWeather(player, storm);
            }
        }
    }

    public static void onLeave(ServerPlayer player) {
        TIME.remove(player.getUUID());
        WEATHER.remove(player.getUUID());
    }

    public static void clear() {
        TIME.clear();
        WEATHER.clear();
    }
}
