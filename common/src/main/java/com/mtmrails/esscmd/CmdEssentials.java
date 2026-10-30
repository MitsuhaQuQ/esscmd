package com.mtmrails.esscmd;

import java.io.IOException;
import com.mojang.brigadier.CommandDispatcher;
import com.mtmrails.esscmd.command.BackCommand;
import com.mtmrails.esscmd.command.EssentialsCommand;
import com.mtmrails.esscmd.command.HomeCommands;
import com.mtmrails.esscmd.command.SpawnCommands;
import com.mtmrails.esscmd.command.SpeedCommand;
import com.mtmrails.esscmd.command.TpaCommands;
import com.mtmrails.esscmd.command.WarpCommands;
import com.mtmrails.esscmd.compat.Players;
import com.mtmrails.esscmd.config.ConfigManager;
import com.mtmrails.esscmd.data.EssData;
import com.mtmrails.esscmd.data.StoredLocation;
import com.mtmrails.esscmd.platform.Platform;
import com.mtmrails.esscmd.teleport.TeleportService;
import com.mtmrails.esscmd.teleport.TpaRequests;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Loader-independent entry point. Loader modules forward their events here and do nothing else. */
public final class CmdEssentials {
    public static final String MOD_ID = "cmdessentials";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private CmdEssentials() {}

    public static void init() {
        LOGGER.info("cmdEssentials {} loading on {}", Platform.get().modVersion(), Platform.get().loaderName());
        try {
            ConfigManager.load();
        } catch (IOException e) {
            LOGGER.error("Could not load the cmdEssentials config, using defaults", e);
        }
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        EssentialsCommand.register(dispatcher);
        HomeCommands.register(dispatcher);
        SpawnCommands.register(dispatcher);
        SpeedCommand.register(dispatcher);
        BackCommand.register(dispatcher);
        TpaCommands.register(dispatcher);
        WarpCommands.register(dispatcher);
    }

    public static void onServerStopping(MinecraftServer server) {
        TeleportService.clear();
        TpaRequests.clear();
    }

    public static void onServerTick(MinecraftServer server) {
        TeleportService.tick(server);
        TpaRequests.tick(server);
    }

    public static void onPlayerJoin(ServerPlayer player) {
        if (ConfigManager.get().spawn.firstJoinAtSpawn && Players.isFirstJoin(player)) {
            StoredLocation spawn = EssData.get(player.server).spawn();
            if (spawn != null) {
                TeleportService.teleportNow(player, spawn);
            }
        }
    }

    public static void onPlayerLeave(ServerPlayer player) {
        TeleportService.cancel(player);
        TpaRequests.forget(player.getUUID());
    }

    /** Called with the dead player, still at the place of death. */
    public static void onPlayerDeath(ServerPlayer player) {
        if (ConfigManager.get().back.saveOnDeath) {
            EssData.get(player.server).setBack(player.getUUID(), StoredLocation.of(player));
        }
    }

    /** Called with the new player entity after a respawn from death (not after leaving the End). */
    public static void onPlayerRespawn(ServerPlayer player) {
        if (ConfigManager.get().spawn.respawnAtSpawn && player.getRespawnPosition() == null) {
            StoredLocation spawn = EssData.get(player.server).spawn();
            if (spawn != null) {
                // Not via TeleportService: a respawn shouldn't overwrite the death location in /back.
                var level = spawn.level(player.server);
                if (level != null) {
                    Players.teleport(player, level, spawn.x(), spawn.y(), spawn.z(), spawn.yaw(), spawn.pitch());
                }
            }
        }
    }
}
