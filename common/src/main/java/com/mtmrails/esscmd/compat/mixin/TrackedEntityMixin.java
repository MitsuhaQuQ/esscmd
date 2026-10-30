package com.mtmrails.esscmd.compat.mixin;

import com.mtmrails.esscmd.compat.VanishTracker;
import com.mtmrails.esscmd.player.VanishService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Stops vanished players being sent to clients that may not see them. */
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class TrackedEntityMixin implements VanishTracker {
    @Shadow
    @Final
    Entity entity;

    @Shadow
    public abstract void removePlayer(ServerPlayer player);

    @Shadow
    public abstract void updatePlayer(ServerPlayer player);

    @Inject(method = "updatePlayer", at = @At("HEAD"), cancellable = true)
    private void cmdessentials$hideVanished(ServerPlayer viewer, CallbackInfo ci) {
        if (entity instanceof ServerPlayer target && VanishService.hiddenFrom(target, viewer)) {
            removePlayer(viewer);
            ci.cancel();
        }
    }

    @Override
    public void cmdessentials$refresh(ServerPlayer viewer) {
        updatePlayer(viewer);
    }
}
