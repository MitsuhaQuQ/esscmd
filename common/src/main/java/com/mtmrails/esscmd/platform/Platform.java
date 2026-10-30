package com.mtmrails.esscmd.platform;

import java.nio.file.Path;
import java.util.ServiceLoader;
import net.minecraft.commands.CommandSourceStack;

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
