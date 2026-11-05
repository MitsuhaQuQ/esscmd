package com.mtmrails.esscmd.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
//? if >=26.1 {
/*import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
*///?} else {
import net.minecraft.world.entity.projectile.AbstractArrow;
//?}

/** Level and entity helpers whose vanilla API differs between versions. */
public final class Worlds {
    private Worlds() {}

    public static int minY(ServerLevel level) {
        //? if >=26.1 {
        /*return level.getMinY();
        *///?} else {
        return level.getMinBuildHeight();
        //?}
    }

    /** The first height above the buildable range. */
    public static int maxY(ServerLevel level) {
        //? if >=26.1 {
        /*return level.getMinY() + level.getHeight();
        *///?} else {
        return level.getMaxBuildHeight();
        //?}
    }

    public static BlockPos spawnPos(ServerLevel level) {
        //? if >=26.1 {
        /*return level.getRespawnData().pos();
        *///?} else {
        return level.getSharedSpawnPos();
        //?}
    }

    public static float spawnAngle(ServerLevel level) {
        //? if >=26.1 {
        /*return level.getRespawnData().yaw();
        *///?} else {
        return level.getSharedSpawnAngle();
        //?}
    }

    public static void setWeather(ServerLevel level, int clearTicks, int rainTicks, boolean raining, boolean thundering) {
        //? if >=26.1 {
        /*level.getServer().setWeatherParameters(clearTicks, rainTicks, raining, thundering);
        *///?} else {
        level.setWeatherParameters(clearTicks, rainTicks, raining, thundering);
        //?}
    }

    public static <T extends Entity> T create(EntityType<T> type, ServerLevel level) {
        //? if >=26.1 {
        /*return type.create(level, EntitySpawnReason.COMMAND);
        *///?} else {
        return type.create(level);
        //?}
    }

    /** Moves a freshly created entity to its spawn point. */
    public static void place(Entity entity, Vec3 position, float yaw) {
        //? if >=26.1 {
        /*entity.snapTo(position.x, position.y, position.z, yaw, 0.0F);
        *///?} else {
        entity.moveTo(position.x, position.y, position.z, yaw, 0.0F);
        //?}
    }

    public static boolean isArrow(Entity entity) {
        return entity instanceof AbstractArrow;
    }
}
