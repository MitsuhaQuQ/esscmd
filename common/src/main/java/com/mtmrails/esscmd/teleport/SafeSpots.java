package com.mtmrails.esscmd.teleport;

import java.util.Optional;
import java.util.Set;
import com.mtmrails.esscmd.compat.Dimensions;
import com.mtmrails.esscmd.data.StoredLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Finding a place a player can stand: a sturdy, harmless floor with two free, dry blocks above.
 * Uses collision shapes rather than {@code blocksMotion()}, which 1.18.2 doesn't have.
 */
public final class SafeSpots {
    private static final Set<Block> HARMFUL_FLOORS = Set.of(Blocks.LAVA, Blocks.MAGMA_BLOCK, Blocks.CACTUS,
            Blocks.FIRE, Blocks.SOUL_FIRE, Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE, Blocks.SWEET_BERRY_BUSH,
            Blocks.POWDER_SNOW, Blocks.WITHER_ROSE, Blocks.POINTED_DRIPSTONE);

    private SafeSpots() {}

    /** True if a player standing with feet at {@code feet} is safe. */
    public static boolean isSafe(ServerLevel level, BlockPos feet) {
        BlockPos floor = feet.below();
        BlockState floorState = level.getBlockState(floor);
        if (HARMFUL_FLOORS.contains(floorState.getBlock()) || !floorState.isFaceSturdy(level, floor, Direction.UP)) {
            return false;
        }
        return isFree(level, feet) && isFree(level, feet.above());
    }

    private static boolean isFree(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getCollisionShape(level, pos).isEmpty() && state.getFluidState().isEmpty()
                && !HARMFUL_FLOORS.contains(state.getBlock());
    }

    /**
     * A safe spot in the column at x/z. Dimensions with a ceiling (the Nether) are scanned
     * downwards from below the roof; others use the surface heightmap. Loads the chunk.
     */
    public static Optional<BlockPos> inColumn(ServerLevel level, int x, int z) {
        int min = level.getMinBuildHeight() + 1;
        if (level.dimensionType().hasCeiling()) {
            int top = Math.min(level.getMaxBuildHeight(), level.getMinBuildHeight() + level.dimensionType().logicalHeight()) - 2;
            for (int y = top; y >= min; y--) {
                BlockPos feet = new BlockPos(x, y, z);
                if (isSafe(level, feet)) {
                    return Optional.of(feet);
                }
            }
            return Optional.empty();
        }
        BlockPos feet = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        return feet.getY() > min && isSafe(level, feet) ? Optional.of(feet) : Optional.empty();
    }

    /** The lowest safe spot in the column at x/z, for /bottom. */
    public static Optional<BlockPos> lowestInColumn(ServerLevel level, int x, int z, int maxY) {
        for (int y = level.getMinBuildHeight() + 1; y <= maxY; y++) {
            BlockPos feet = new BlockPos(x, y, z);
            if (isSafe(level, feet)) {
                return Optional.of(feet);
            }
        }
        return Optional.empty();
    }

    /** The first safe spot at or above {@code start}, within {@code range} blocks, for /jump. */
    public static Optional<BlockPos> above(ServerLevel level, BlockPos start, int range) {
        int top = level.getMaxBuildHeight() - 2;
        for (int y = start.getY(); y <= Math.min(top, start.getY() + range); y++) {
            BlockPos feet = new BlockPos(start.getX(), y, start.getZ());
            if (isSafe(level, feet)) {
                return Optional.of(feet);
            }
        }
        return Optional.empty();
    }

    /** Centre of the block, keeping the given rotation. */
    public static StoredLocation location(ServerLevel level, BlockPos feet, float yaw, float pitch) {
        return new StoredLocation(Dimensions.id(level.dimension()), feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, yaw, pitch);
    }
}
