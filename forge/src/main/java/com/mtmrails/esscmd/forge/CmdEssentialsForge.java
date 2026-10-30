package com.mtmrails.esscmd.forge;

import com.mtmrails.esscmd.CmdEssentials;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.nodes.PermissionNode;
//? if <1.20 {
/*import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.network.NetworkConstants;
*///?}

/** Forwards Forge events to {@link CmdEssentials}; no logic of its own. */
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
        MinecraftForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> CmdEssentials.onServerStopping(event.getServer()));
        //? if >=1.21 {
        /*MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent.Post event) ->
                CmdEssentials.onServerTick(event.getServer()));
        *///?} else {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END) {
                CmdEssentials.onServerTick(server(event));
            }
        });
        //?}
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (player(event) instanceof ServerPlayer player) {
                CmdEssentials.onPlayerJoin(player);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (player(event) instanceof ServerPlayer player) {
                CmdEssentials.onPlayerLeave(player);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((LivingDeathEvent event) -> {
            if (entity(event) instanceof ServerPlayer player) {
                CmdEssentials.onPlayerDeath(player);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event) -> {
            if (!event.isEndConquered() && player(event) instanceof ServerPlayer player) {
                CmdEssentials.onPlayerRespawn(player);
            }
        });
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        CmdEssentials.registerCommands(event.getDispatcher());
    }

    private void onGatherPermissionNodes(PermissionGatherEvent.Nodes event) {
        event.addNodes(ForgePlatform.NODES.values().toArray(new PermissionNode<?>[0]));
        event.addNodes(ForgePlatform.INT_NODES.values().toArray(new PermissionNode<?>[0]));
    }

    private static MinecraftServer server(TickEvent.ServerTickEvent event) {
        //? if >=1.20 {
        return event.getServer();
        //?} else {
        /*return ServerLifecycleHooks.getCurrentServer();
        *///?}
    }

    // Forge renamed getPlayer()/getEntityLiving() to getEntity() in 1.19.

    private static Entity player(PlayerEvent event) {
        //? if >=1.19 {
        return event.getEntity();
        //?} else {
        /*return event.getPlayer();
        *///?}
    }

    private static Entity entity(LivingDeathEvent event) {
        //? if >=1.19 {
        return event.getEntity();
        //?} else {
        /*return event.getEntityLiving();
        *///?}
    }
}
