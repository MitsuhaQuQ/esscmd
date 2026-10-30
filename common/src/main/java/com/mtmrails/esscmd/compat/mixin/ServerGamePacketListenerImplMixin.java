package com.mtmrails.esscmd.compat.mixin;

import com.mtmrails.esscmd.player.AfkService;
import com.mtmrails.esscmd.player.Mutes;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=1.19 {
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
//?}
//? if >=1.19.3 {
import net.minecraft.network.chat.PlayerChatMessage;
//?} else {
/*import net.minecraft.server.network.TextFilter;
*///?}
//? if >=1.20.5 {
/*import net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket;
*///?}

/**
 * Counts chat and commands as activity for AFK. These handlers run on the network thread on
 * 1.19+, so the hook only records a timestamp. Before 1.19 commands arrive as chat packets.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleChat", at = @At("HEAD"))
    private void cmdessentials$onChat(ServerboundChatPacket packet, CallbackInfo ci) {
        AfkService.markActive(player);
    }

    //? if >=1.19 {
    @Inject(method = "handleChatCommand", at = @At("HEAD"))
    private void cmdessentials$onCommand(ServerboundChatCommandPacket packet, CallbackInfo ci) {
        AfkService.markActive(player);
    }
    //?}

    // /mute stops the broadcast, not the packet: on 1.19+ dropping a chat packet would desync
    // signed chat's last-seen tracking and get the player kicked.
    //? if >=1.19.3 {
    @Inject(method = "broadcastChatMessage", at = @At("HEAD"), cancellable = true)
    private void cmdessentials$onBroadcast(PlayerChatMessage message, CallbackInfo ci) {
        if (Mutes.blockChat(player)) {
            ci.cancel();
        }
    }
    //?} else {
    /*@Inject(method = "handleChat(Lnet/minecraft/server/network/TextFilter$FilteredText;)V", at = @At("HEAD"), cancellable = true)
    private void cmdessentials$onFilteredChat(TextFilter.FilteredText message, CallbackInfo ci) {
        if (!message.getRaw().startsWith("/") && Mutes.blockChat(player)) {
            ci.cancel();
        }
    }
    *///?}

    //? if >=1.20.5 {
    /*@Inject(method = "handleSignedChatCommand", at = @At("HEAD"))
    private void cmdessentials$onSignedCommand(ServerboundChatCommandSignedPacket packet, CallbackInfo ci) {
        AfkService.markActive(player);
    }
    *///?}
}
