package com.mtmrails.esscmd.teleport;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import com.mtmrails.esscmd.compat.Dimensions;
import com.mtmrails.esscmd.config.ConfigManager;
import com.mtmrails.esscmd.config.EssConfig;
import com.mtmrails.esscmd.data.StoredLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * Picks a random safe spot for /tpr inside the dimension's configured ring
 * ({@code tpr.areas}: centre, min and max range). Each attempt loads (and maybe generates) one
 * chunk on the server thread, so {@code tpr.attempts} keeps the worst case bounded.
 */
public final class RandomTeleport {
    private RandomTeleport() {}

    /** The area for a dimension, or null if /tpr isn't enabled there. */
    @Nullable
    public static EssConfig.TprArea area(ServerLevel level) {
        return ConfigManager.get().tpr.areas.get(Dimensions.id(level.dimension()));
    }

    public static Optional<StoredLocation> find(ServerLevel level, EssConfig.TprArea area) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int attempts = Math.max(1, ConfigManager.get().tpr.attempts);
        double min = Math.max(0, area.minRange);
        double max = Math.max(min + 1, area.maxRange);
        for (int i = 0; i < attempts; i++) {
            // Uniform over the ring's area, not its radius.
            double radius = Math.sqrt(random.nextDouble(min * min, max * max));
            double angle = random.nextDouble(Math.PI * 2);
            int x = (int) Math.floor(area.centerX + radius * Math.cos(angle));
            int z = (int) Math.floor(area.centerZ + radius * Math.sin(angle));
            if (!level.getWorldBorder().isWithinBounds(new BlockPos(x, 0, z))) {
                continue;
            }
            Optional<BlockPos> spot = SafeSpots.inColumn(level, x, z);
            if (spot.isPresent()) {
                return Optional.of(SafeSpots.location(level, spot.get(), random.nextFloat() * 360 - 180, 0));
            }
        }
        return Optional.empty();
    }
}
