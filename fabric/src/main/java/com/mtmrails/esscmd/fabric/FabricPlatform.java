package com.mtmrails.esscmd.fabric;

import java.nio.file.Path;
import com.mtmrails.esscmd.CmdEssentials;
import com.mtmrails.esscmd.permission.IntOption;
import com.mtmrails.esscmd.platform.Platform;
import me.lucko.fabric.api.permissions.v0.Options;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

public final class FabricPlatform implements Platform {
    private static final boolean PERMISSIONS_API = FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0");

    @Override
    public String loaderName() {
        return "Fabric";
    }

    @Override
    public String modVersion() {
        return FabricLoader.getInstance().getModContainer(CmdEssentials.MOD_ID)
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public boolean hasPermission(CommandSourceStack source, String node, int opLevel) {
        return PERMISSIONS_API ? LuckoPermissions.check(source, node, opLevel) : source.hasPermission(opLevel);
    }

    @Override
    public int getInt(ServerPlayer player, IntOption option) {
        int fallback = option.fallback().getAsInt();
        return PERMISSIONS_API ? LuckoPermissions.getInt(player, option.name(), fallback) : fallback;
    }

    /** Kept in its own class so fabric-permissions-api is only loaded when it is installed. */
    private static final class LuckoPermissions {
        static boolean check(CommandSourceStack source, String node, int opLevel) {
            return Permissions.check(source, node, opLevel);
        }

        static int getInt(ServerPlayer player, String key, int fallback) {
            try {
                return Options.get(player.createCommandSourceStack(), key, fallback, Integer::parseInt);
            } catch (NumberFormatException e) {
                return fallback;
            }
        }
    }
}
