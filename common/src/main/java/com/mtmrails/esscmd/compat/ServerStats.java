package com.mtmrails.esscmd.compat;

import net.minecraft.server.MinecraftServer;

/** Server performance numbers; the tick-time API changed in 1.20.3. */
public final class ServerStats {
    private ServerStats() {}

    public static double averageTickMillis(MinecraftServer server) {
        //? if >=1.20.3 {
        /*return server.getAverageTickTimeNanos() / 1_000_000.0;
        *///?} else {
        return server.getAverageTickTime();
        //?}
    }
}
