package com.mtmrails.esscmd.teleport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.mtmrails.esscmd.compat.Players;
import com.mtmrails.esscmd.message.Messages;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/** Pending /tpa and /tpahere requests, kept in memory only. Server thread only. */
public final class TpaRequests {
    public enum Type {
        /** /tpa: the requester goes to the target. */
        TO_TARGET,
        /** /tpahere: the target comes to the requester. */
        HERE
    }

    public record Request(UUID requester, String requesterName, UUID target, String targetName, Type type, int expiresTick) {}

    /** target -> (requester -> request), oldest first. One request per requester and target. */
    private static final Map<UUID, LinkedHashMap<UUID, Request>> BY_TARGET = new HashMap<>();

    private TpaRequests() {}

    public static void add(Request request) {
        LinkedHashMap<UUID, Request> forTarget = BY_TARGET.computeIfAbsent(request.target(), id -> new LinkedHashMap<>());
        forTarget.remove(request.requester());
        forTarget.put(request.requester(), request);
    }

    /** Removes and returns the newest request to {@code target}, or the one from {@code requester}. */
    @Nullable
    public static Request take(UUID target, @Nullable UUID requester) {
        LinkedHashMap<UUID, Request> forTarget = BY_TARGET.get(target);
        if (forTarget == null || forTarget.isEmpty()) {
            return null;
        }
        Request request;
        if (requester != null) {
            request = forTarget.remove(requester);
        } else {
            UUID newest = null;
            for (UUID id : forTarget.keySet()) {
                newest = id;
            }
            request = forTarget.remove(newest);
        }
        if (forTarget.isEmpty()) {
            BY_TARGET.remove(target);
        }
        return request;
    }

    /** Removes the requests {@code requester} sent, to everyone or only to {@code target}. */
    public static List<Request> cancelFrom(UUID requester, @Nullable UUID target) {
        List<Request> removed = new ArrayList<>();
        Iterator<Map.Entry<UUID, LinkedHashMap<UUID, Request>>> it = BY_TARGET.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, LinkedHashMap<UUID, Request>> entry = it.next();
            if (target != null && !entry.getKey().equals(target)) {
                continue;
            }
            Request request = entry.getValue().remove(requester);
            if (request != null) {
                removed.add(request);
            }
            if (entry.getValue().isEmpty()) {
                it.remove();
            }
        }
        return removed;
    }

    /** Drops everything involving a player who left. */
    public static void forget(UUID player) {
        BY_TARGET.remove(player);
        cancelFrom(player, null);
    }

    public static void tick(MinecraftServer server) {
        if (BY_TARGET.isEmpty()) {
            return;
        }
        int now = server.getTickCount();
        Iterator<LinkedHashMap<UUID, Request>> targets = BY_TARGET.values().iterator();
        while (targets.hasNext()) {
            LinkedHashMap<UUID, Request> forTarget = targets.next();
            Iterator<Request> requests = forTarget.values().iterator();
            while (requests.hasNext()) {
                Request request = requests.next();
                if (now >= request.expiresTick()) {
                    requests.remove();
                    ServerPlayer requester = server.getPlayerList().getPlayer(request.requester());
                    if (requester != null) {
                        Players.send(requester, Messages.error(requester, "tpa.expired", request.targetName()));
                    }
                }
            }
            if (forTarget.isEmpty()) {
                targets.remove();
            }
        }
    }

    public static void clear() {
        BY_TARGET.clear();
    }
}
