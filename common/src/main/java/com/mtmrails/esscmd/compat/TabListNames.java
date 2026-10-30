package com.mtmrails.esscmd.compat;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
//? if >=1.19.3 {
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
//?} else {
/*import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
*///?}

/** Sends the version-specific packet that refreshes one player's tab-list display name. */
public final class TabListNames {
    private TabListNames() {}

    /** Removes {@code player} from {@code viewer}'s tab list. */
    public static void remove(ServerPlayer viewer, ServerPlayer player) {
        //? if >=1.19.3 {
        viewer.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(player.getUUID())));
        //?} else {
        /*viewer.connection.send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.REMOVE_PLAYER, player));
        *///?}
    }

    /** Adds {@code player} to {@code viewer}'s tab list. */
    public static void add(ServerPlayer viewer, ServerPlayer player) {
        //? if >=1.19.3 {
        viewer.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(player)));
        //?} else {
        /*viewer.connection.send(new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.ADD_PLAYER, player));
        *///?}
    }

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
