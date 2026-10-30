package com.mtmrails.esscmd.platform;

import java.nio.file.Path;
import java.util.ServiceLoader;
import com.mtmrails.esscmd.permission.IntOption;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

/**
 * Everything common code needs from the mod loader. Each loader module provides one
 * implementation, registered in {@code META-INF/services/com.mtmrails.esscmd.platform.Platform}.
 */
public interface Platform {
    String loaderName();

    String modVersion();

    Path configDir();

    /** Checks a permission node, falling back to {@code opLevel} when no permission provider is installed. */
    boolean hasPermission(CommandSourceStack source, String node, int opLevel);

    /** Reads a numeric option from the permission provider, or {@link IntOption#fallback()}. */
    int getInt(ServerPlayer player, IntOption option);

    static Platform get() {
        return Holder.INSTANCE;
    }

    final class Holder {
        private static final Platform INSTANCE = ServiceLoader.load(Platform.class, Platform.class.getClassLoader())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No Platform implementation found"));

        private Holder() {}
    }
}
