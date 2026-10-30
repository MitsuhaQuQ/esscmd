package com.mtmrails.esscmd.compat.mixin;

//? if <1.20.2 {
import com.mtmrails.esscmd.compat.ClientLanguage;
import net.minecraft.network.protocol.game.ServerboundClientInformationPacket;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Remembers the client's language, which vanilla only keeps from 1.20.2 on. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin implements ClientLanguage {
    @Unique
    private String cmdessentials$language = "en_us";

    @Inject(method = "updateOptions", at = @At("TAIL"))
    private void cmdessentials$rememberLanguage(ServerboundClientInformationPacket packet, CallbackInfo ci) {
        cmdessentials$language = packet.language();
    }

    @Override
    public String cmdessentials$getLanguage() {
        return cmdessentials$language;
    }
}
//?} else {
/*import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;

// Nothing to do from 1.20.2: vanilla keeps the language in clientInformation().
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
}
*///?}
