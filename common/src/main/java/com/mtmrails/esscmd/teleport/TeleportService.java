package com.mtmrails.esscmd.teleport;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import com.mtmrails.esscmd.compat.Players;
import com.mtmrails.esscmd.config.ConfigManager;
import com.mtmrails.esscmd.config.EssConfig;
import com.mtmrails.esscmd.data.EssData;
import com.mtmrails.esscmd.data.StoredLocation;
import com.mtmrails.esscmd.message.Messages;
import com.mtmrails.esscmd.permission.PermissionNodes;
import com.mtmrails.esscmd.player.JailService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Every teleport goes through here: warm-up delay, cancelling on movement, cooldowns and
 * saving the /back location. Server thread only.
 */
public final class TeleportService {
    private static final double MOVE_TOLERANCE_SQR = 0.5 * 0.5;

    /** {@code target} is evaluated when the teleport happens, so it can follow a moving player. */
    private record Pending(Supplier<StoredLocation> target, Vec3 start, int dueTick) {}

    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    private static final Map<UUID, Integer> COOLDOWN_UNTIL = new HashMap<>();

    private TeleportService() {}

    /**
     * A teleport the player asked for themselves: subject to cooldown and delay unless they
     * have the bypass nodes. Sends its own feedback. Returns false if it was refused.
     */
    public static boolean teleport(ServerPlayer player, Supplier<StoredLocation> target) {
        if (JailService.isJailed(player)) {
            Players.send(player, Messages.error(player, "jail.no-teleport"));
            return false;
        }
        EssConfig.Teleport config = ConfigManager.get().teleport;
        int now = player.server.getTickCount();
        Integer until = COOLDOWN_UNTIL.get(player.getUUID());
        if (until != null && now < until && !PermissionNodes.TELEPORT_BYPASS_COOLDOWN.check(player)) {
            Players.send(player, Messages.error(player, "teleport.cooldown", (until - now + 19) / 20));
            return false;
        }
        int delayTicks = config.delaySeconds * 20;
        if (delayTicks <= 0 || PermissionNodes.TELEPORT_BYPASS_DELAY.check(player)) {
            PENDING.remove(player.getUUID());
            return perform(player, target.get());
        }
        PENDING.put(player.getUUID(), new Pending(target, player.position(), now + delayTicks));
        Players.send(player, Messages.info(player, config.cancelOnMove ? "teleport.delay-still" : "teleport.delay",
                config.delaySeconds));
        return true;
    }

    /** An immediate teleport done to a player by someone else or by the server. No delay, no cooldown. */
    public static boolean teleportNow(ServerPlayer player, StoredLocation target) {
        PENDING.remove(player.getUUID());
        return perform(player, target);
    }

    public static void cancel(ServerPlayer player) {
        PENDING.remove(player.getUUID());
    }

    public static void tick(MinecraftServer server) {
        if (PENDING.isEmpty()) {
            return;
        }
        int now = server.getTickCount();
        boolean cancelOnMove = ConfigManager.get().teleport.cancelOnMove;
        Iterator<Map.Entry<UUID, Pending>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Pending> entry = it.next();
            Pending pending = entry.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                it.remove();
            } else if (cancelOnMove && player.position().distanceToSqr(pending.start()) > MOVE_TOLERANCE_SQR) {
                it.remove();
                Players.send(player, Messages.error(player, "teleport.cancelled"));
            } else if (now >= pending.dueTick()) {
                it.remove();
                perform(player, pending.target().get());
            }
        }
    }

    public static void clear() {
        PENDING.clear();
        COOLDOWN_UNTIL.clear();
    }

    private static boolean perform(ServerPlayer player, @Nullable StoredLocation target) {
        if (target == null) {
            Players.send(player, Messages.error(player, "teleport.target-gone"));
            return false;
        }
        MinecraftServer server = player.server;
        ServerLevel level = target.level(server);
        if (level == null) {
            Players.send(player, Messages.error(player, "teleport.unknown-world", target.dimension()));
            return false;
        }
        EssData.get(server).setBack(player.getUUID(), StoredLocation.of(player));
        Players.teleport(player, level, target.x(), target.y(), target.z(), target.yaw(), target.pitch());
        int cooldown = ConfigManager.get().teleport.cooldownSeconds * 20;
        if (cooldown > 0) {
            COOLDOWN_UNTIL.put(player.getUUID(), server.getTickCount() + cooldown);
        }
        return true;
    }
}
