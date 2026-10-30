package io.github.cmdessentials.fabric;

import java.nio.file.Path;
import io.github.cmdessentials.CmdEssentials;
import io.github.cmdessentials.platform.Platform;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;

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

    /** Kept in its own class so fabric-permissions-api is only loaded when it is installed. */
    private static final class LuckoPermissions {
        static boolean check(CommandSourceStack source, String node, int opLevel) {
            return Permissions.check(source, node, opLevel);
        }
    }
}
