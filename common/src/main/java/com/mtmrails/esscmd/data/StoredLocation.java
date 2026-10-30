package com.mtmrails.esscmd.data;

import com.mtmrails.esscmd.compat.Dimensions;
import com.mtmrails.esscmd.compat.Players;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/** A position in a dimension, with rotation. Used for homes, warps, spawn and /back. */
public record StoredLocation(String dimension, double x, double y, double z, float yaw, float pitch) {
    public static StoredLocation of(ServerPlayer player) {
        return new StoredLocation(Dimensions.id(Players.level(player).dimension()),
                player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
    }

    /** The world spawn of the overworld, centred on its block. */
    public static StoredLocation worldSpawn(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        BlockPos pos = overworld.getSharedSpawnPos();
        return new StoredLocation(Dimensions.id(overworld.dimension()),
                pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, overworld.getSharedSpawnAngle(), 0);
    }

    /** The loaded level for this location, or null if that dimension doesn't exist. */
    @Nullable
    public ServerLevel level(MinecraftServer server) {
        return server.getLevel(Dimensions.key(dimension));
    }

    /** {@code "12, 64, -3 (minecraft:overworld)"} */
    public String describe() {
        return Math.round(Math.floor(x)) + ", " + Math.round(Math.floor(y)) + ", " + Math.round(Math.floor(z)) + " (" + dimension + ")";
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("dim", dimension);
        tag.putDouble("x", x);
        tag.putDouble("y", y);
        tag.putDouble("z", z);
        tag.putFloat("yaw", yaw);
        tag.putFloat("pitch", pitch);
        return tag;
    }

    public static StoredLocation fromTag(CompoundTag tag) {
        return new StoredLocation(tag.getString("dim"), tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z"),
                tag.getFloat("yaw"), tag.getFloat("pitch"));
    }
}
