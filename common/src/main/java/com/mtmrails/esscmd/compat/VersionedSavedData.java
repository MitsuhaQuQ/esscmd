package com.mtmrails.esscmd.compat;

import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
//? if >=1.20.5 {
/*import net.minecraft.core.HolderLookup;
*///?}

/**
 * {@link SavedData} base that hides the version differences: {@code save} gained a registry
 * argument and {@code computeIfAbsent} switched to a {@code Factory} in 1.20.5.
 */
public abstract class VersionedSavedData extends SavedData {
    protected abstract CompoundTag write(CompoundTag tag);

    //? if >=1.20.5 {
    /*@Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        return write(tag);
    }
    *///?} else {
    @Override
    public CompoundTag save(CompoundTag tag) {
        return write(tag);
    }
    //?}

    public static <T extends SavedData> T getOrCreate(ServerLevel level, String name,
                                                      Function<CompoundTag, T> load, Supplier<T> create) {
        //? if >=1.20.5 {
        /*return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(create, (tag, registries) -> load.apply(tag), null), name);
        *///?} else {
        return level.getDataStorage().computeIfAbsent(load, create, name);
        //?}
    }
}
