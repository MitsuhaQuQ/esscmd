package com.mtmrails.esscmd.compat.mixin;

import com.mtmrails.esscmd.message.Nicknames;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies persisted nicknames to the normal player display name. */
@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
    private void cmdessentials$displayName(CallbackInfoReturnable<Component> cir) {
        if ((Object) this instanceof ServerPlayer player) {
            cir.setReturnValue(Nicknames.displayName(player));
        }
    }
}
