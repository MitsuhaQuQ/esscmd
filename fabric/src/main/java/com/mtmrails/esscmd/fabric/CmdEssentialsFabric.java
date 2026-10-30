package com.mtmrails.esscmd.fabric;

import com.mtmrails.esscmd.CmdEssentials;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
//? if >=1.19 {
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
//?} else {
/*import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
*///?}

/** Forwards Fabric events to {@link CmdEssentials}; no logic of its own. */
public final class CmdEssentialsFabric implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        CmdEssentials.init();
        //? if >=1.19 {
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
                CmdEssentials.registerCommands(dispatcher));
        //?} else {
        /*CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) ->
                CmdEssentials.registerCommands(dispatcher));
        *///?}
        ServerLifecycleEvents.SERVER_STOPPING.register(CmdEssentials::onServerStopping);
        ServerTickEvents.END_SERVER_TICK.register(CmdEssentials::onServerTick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> CmdEssentials.onPlayerJoin(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> CmdEssentials.onPlayerLeave(handler.player));
        // Fabric API for 1.18.2 has no death event, so use the respawn: the old entity still
        // stands where the player died. `alive` is true when returning from the End.
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!alive) {
                CmdEssentials.onPlayerDeath(oldPlayer);
                CmdEssentials.onPlayerRespawn(newPlayer);
            }
        });
    }
}
