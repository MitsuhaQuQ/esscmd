package com.mtmrails.esscmd.compat;

import net.minecraft.server.level.ServerPlayer;

/** Added to {@code ChunkMap.TrackedEntity} by {@code TrackedEntityMixin}. */
public interface VanishTracker {
    /** Re-evaluates whether {@code viewer} tracks this entity. */
    void cmdessentials$refresh(ServerPlayer viewer);
}
