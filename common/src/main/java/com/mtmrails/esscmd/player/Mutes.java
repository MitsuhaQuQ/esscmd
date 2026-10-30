package com.mtmrails.esscmd.player;

import com.mtmrails.esscmd.compat.Players;
import com.mtmrails.esscmd.data.EssData;
import com.mtmrails.esscmd.message.Messages;
import net.minecraft.server.level.ServerPlayer;

/** Enforces /mute. Server thread only. */
public final class Mutes {
    private Mutes() {}

    /** True if the player is muted, after telling them so. */
    public static boolean blockChat(ServerPlayer player) {
        if (!EssData.get(player.server).muted(player.getUUID())) {
            return false;
        }
        Players.send(player, Messages.error(player, "mute.blocked"));
        return true;
    }
}
