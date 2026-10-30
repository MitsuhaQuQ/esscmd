package com.mtmrails.esscmd.fabric;

import com.mtmrails.esscmd.CmdEssentials;
import net.fabricmc.api.DedicatedServerModInitializer;
//? if >=1.19 {
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
//?} else {
/*import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
*///?}

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
    }
}
