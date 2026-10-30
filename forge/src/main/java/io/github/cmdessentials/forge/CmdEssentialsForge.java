package io.github.cmdessentials.forge;

import io.github.cmdessentials.CmdEssentials;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.nodes.PermissionNode;
//? if <1.20 {
/*import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.network.NetworkConstants;
*///?}

@Mod(CmdEssentials.MOD_ID)
public final class CmdEssentialsForge {
    public CmdEssentialsForge() {
        //? if <1.20 {
        /*// Older mods.toml has no displayTest key: tell clients they don't need this mod.
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (version, fromServer) -> true));
        *///?}
        CmdEssentials.init();
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onGatherPermissionNodes);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        CmdEssentials.registerCommands(event.getDispatcher());
    }

    private void onGatherPermissionNodes(PermissionGatherEvent.Nodes event) {
        event.addNodes(ForgePlatform.NODES.values().toArray(new PermissionNode<?>[0]));
    }
}
