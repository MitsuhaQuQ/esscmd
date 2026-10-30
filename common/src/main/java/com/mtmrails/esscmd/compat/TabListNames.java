package com.mtmrails.esscmd.compat;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
//? if >=1.19.3 {
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
//?} else {
/*import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
*///?}

/** Sends the version-specific packet that refreshes one player's tab-list display name. */
public final class TabListNames {
    private TabListNames() {}

    public static void refresh(ServerPlayer player) {
        //? if >=1.19.3 {
        var packet = new ClientboundPlayerInfoUpdatePacket(
                EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME), List.of(player));
        //?} else {
        /*var packet = new ClientboundPlayerInfoPacket(
                ClientboundPlayerInfoPacket.Action.UPDATE_DISPLAY_NAME, player);
        *///?}
        player.server.getPlayerList().broadcastAll(packet);
    }
}
