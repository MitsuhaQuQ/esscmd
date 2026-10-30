package com.mtmrails.esscmd;

import java.io.IOException;
import com.mojang.brigadier.CommandDispatcher;
import com.mtmrails.esscmd.command.AfkCommand;
import com.mtmrails.esscmd.command.BackCommand;
import com.mtmrails.esscmd.command.BroadcastCommands;
import com.mtmrails.esscmd.command.EssentialsCommand;
import com.mtmrails.esscmd.command.HomeCommands;
import com.mtmrails.esscmd.command.LocationInfoCommands;
import com.mtmrails.esscmd.command.MailCommands;
import com.mtmrails.esscmd.command.MessagingCommands;
import com.mtmrails.esscmd.command.NickCommands;
import com.mtmrails.esscmd.command.PlayerInfoCommands;
import com.mtmrails.esscmd.command.PlayerStateCommands;
import com.mtmrails.esscmd.command.SpawnCommands;
import com.mtmrails.esscmd.command.SpeedCommand;
import com.mtmrails.esscmd.command.TeleportExtraCommands;
import com.mtmrails.esscmd.command.TextCommands;
import com.mtmrails.esscmd.command.TpaCommands;
import com.mtmrails.esscmd.command.VanillaAliases;
import com.mtmrails.esscmd.command.WarpCommands;
import com.mtmrails.esscmd.compat.Players;
import com.mtmrails.esscmd.config.ConfigManager;
import com.mtmrails.esscmd.data.EssData;
import com.mtmrails.esscmd.data.StoredLocation;
import com.mtmrails.esscmd.platform.Platform;
import com.mtmrails.esscmd.message.MessagingService;
import com.mtmrails.esscmd.message.Messages;
import com.mtmrails.esscmd.player.AbilityService;
import com.mtmrails.esscmd.player.AfkService;
import com.mtmrails.esscmd.player.Sessions;
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
        }        if (Boolean.getBoolean("cmdessentials.debug.loadMixinTargets")) {
            loadMixinTargets();
        }
    }

    /**
     * Testing aid: mixin targets such as the network handler normally load only when a player
     * joins, so a broken injection would crash then. With
     * {@code -Dcmdessentials.debug.loadMixinTargets=true} they load at startup instead.
     */
    private static void loadMixinTargets() {
        for (String target : new String[] {
                "net.minecraft.server.level.ServerPlayer",
                "net.minecraft.server.network.ServerGamePacketListenerImpl"}) {
            try {
                Class.forName(target, true, CmdEssentials.class.getClassLoader());
                LOGGER.info("Mixin target {} loaded", target);
            } catch (ReflectiveOperationException | LinkageError e) {
                throw new IllegalStateException("Mixin target " + target + " failed to load", e);
            }
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
        PlayerStateCommands.register(dispatcher);
        AfkCommand.register(dispatcher);
        VanillaAliases.register(dispatcher);
        LocationInfoCommands.register(dispatcher);
        PlayerInfoCommands.register(dispatcher);
        TextCommands.register(dispatcher);
        TeleportExtraCommands.register(dispatcher);
        MessagingCommands.register(dispatcher);
        MailCommands.register(dispatcher);
        BroadcastCommands.register(dispatcher);
        NickCommands.register(dispatcher);
    }

    public static void onServerStopping(MinecraftServer server) {
        TeleportService.clear();
        TpaRequests.clear();
        AfkService.clear();
        Sessions.clear();
        MessagingService.clear();
    }

    public static void onServerTick(MinecraftServer server) {
        TeleportService.tick(server);
        TpaRequests.tick(server);
        AbilityService.tick(server);
        AfkService.tick(server);
    }

    public static void onPlayerJoin(ServerPlayer player) {
        EssData.get(player.server).playerJoined(player.getUUID(), player.getGameProfile().getName());
        AfkService.onJoin(player);
        Sessions.onJoin(player);
        int unreadMail = EssData.get(player.server).unreadMail(player.getUUID(), System.currentTimeMillis());
        if (unreadMail > 0) {
            Players.send(player, Messages.info(player, "mail.unread", unreadMail));
        }
        if (ConfigManager.get().motd.showOnJoin) {
            TextCommands.showMotd(player);
        }
        if (ConfigManager.get().spawn.firstJoinAtSpawn && Players.isFirstJoin(player)) {
            StoredLocation spawn = EssData.get(player.server).spawn();
            if (spawn != null) {
                TeleportService.teleportNow(player, spawn);
            }
        }
    }

    public static void onPlayerLeave(ServerPlayer player) {
        EssData.get(player.server).playerLeft(player.getUUID(), player.getGameProfile().getName(),
                StoredLocation.of(player), System.currentTimeMillis());
        TeleportService.cancel(player);
        TpaRequests.forget(player.getUUID());
        AfkService.onLeave(player);
        Sessions.onLeave(player);
        MessagingService.forget(player.getUUID());
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
