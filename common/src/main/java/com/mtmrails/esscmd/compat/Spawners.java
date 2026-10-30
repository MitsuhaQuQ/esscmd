package com.mtmrails.esscmd.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

/** Changing a spawner's mob; the method takes the level from 1.20. */
public final class Spawners {
    private Spawners() {}

    public static void setMob(SpawnerBlockEntity spawner, EntityType<?> type, ServerLevel level, BlockPos pos) {
        //? if >=1.20 {
        spawner.getSpawner().setEntityId(type, level, level.getRandom(), pos);
        //?} else {
        /*spawner.getSpawner().setEntityId(type);
        *///?}
    }
}
