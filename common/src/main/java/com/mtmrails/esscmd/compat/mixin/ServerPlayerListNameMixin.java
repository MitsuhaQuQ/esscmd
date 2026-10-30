package com.mtmrails.esscmd.compat.mixin;

import com.mtmrails.esscmd.message.Nicknames;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Supplies nickname and AFK state to the vanilla tab-list packet. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerListNameMixin {
    @Inject(method = "getTabListDisplayName", at = @At("HEAD"), cancellable = true)
    private void cmdessentials$tabListName(CallbackInfoReturnable<Component> cir) {
        cir.setReturnValue(Nicknames.tabListName((ServerPlayer) (Object) this));
    }
}
