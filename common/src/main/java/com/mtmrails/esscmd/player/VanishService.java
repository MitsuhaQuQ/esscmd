package com.mtmrails.esscmd.player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import com.mtmrails.esscmd.compat.Players;
import com.mtmrails.esscmd.compat.TabListNames;
import com.mtmrails.esscmd.compat.VanishTracker;
import com.mtmrails.esscmd.compat.mixin.ChunkMapAccessor;
import com.mtmrails.esscmd.permission.PermissionNodes;
import net.minecraft.server.level.ServerPlayer;

/**
 * /vanish. Hidden players aren't sent to other clients at all: {@code TrackedEntityMixin}
 * filters the entity tracker and the tab list entry is removed. Not persisted; a vanished
 * player who leaves is visible again next time.
 */
public final class VanishService {
    private static final Set<UUID> VANISHED = new HashSet<>();

    private VanishService() {}

    public static boolean isVanished(ServerPlayer player) {
        return VANISHED.contains(player.getUUID());
    }

    /** True if {@code viewer} must not see {@code target}. */
    public static boolean hiddenFrom(ServerPlayer target, ServerPlayer viewer) {
        return target != viewer && isVanished(target) && !PermissionNodes.VANISH_SEE.check(viewer);
    }

    public static void set(ServerPlayer player, boolean vanished) {
        if (vanished) {
            VANISHED.add(player.getUUID());
        } else {
            VANISHED.remove(player.getUUID());
        }
        Object tracked = ((ChunkMapAccessor) Players.level(player).getChunkSource().chunkMap)
                .cmdessentials$entityMap().get(player.getId());
        for (ServerPlayer viewer : player.server.getPlayerList().getPlayers()) {
            if (viewer == player) {
                continue;
            }
            if (hiddenFrom(player, viewer)) {
                TabListNames.remove(viewer, player);
            } else {
                TabListNames.add(viewer, player);
            }
            if (tracked != null) {
                ((VanishTracker) tracked).cmdessentials$refresh(viewer);
            }
        }
    }

    /** A new player's client just received the whole tab list; take the hidden ones out. */
    public static void onJoin(ServerPlayer joined) {
        for (ServerPlayer other : joined.server.getPlayerList().getPlayers()) {
            if (hiddenFrom(other, joined)) {
                TabListNames.remove(joined, other);
            }
        }
    }

    public static void onLeave(ServerPlayer player) {
        VANISHED.remove(player.getUUID());
    }

    public static void clear() {
        VANISHED.clear();
    }
}
