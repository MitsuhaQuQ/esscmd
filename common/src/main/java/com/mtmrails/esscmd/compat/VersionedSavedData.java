package com.mtmrails.esscmd.compat;

import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
//? if >=26.1 {
/*import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;
*///?} else if >=1.20.5 {
/*import net.minecraft.core.HolderLookup;
*///?}

/**
 * {@link SavedData} base that hides the version differences: {@code save} gained a registry
 * argument and {@code computeIfAbsent} switched to a {@code Factory} in 1.20.5.
 */
public abstract class VersionedSavedData extends SavedData {
    protected abstract CompoundTag write(CompoundTag tag);

    //? if >=26.1 {
    /*/^* Saved data went through codecs from 1.21.5; only the tag form is needed here. ^/
    private CompoundTag encode() {
        return write(new CompoundTag());
    }
    *///?} else if >=1.20.5 {
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

    //? if >=26.1 {
    /*/^* 26.1 moved saved data, and vanilla's world upgrade doesn't know about ours: read the old file once. ^/
    private static <T extends SavedData> T migrate(ServerLevel level, String name, Function<CompoundTag, T> load, Supplier<T> create) {
        Path old = level.getServer().getWorldPath(LevelResource.ROOT).resolve("data").resolve(name + ".dat");
        if (!Files.exists(old)) {
            return create.get();
        }
        try {
            T data = load.apply(Tags.compound(NbtIo.readCompressed(old, NbtAccounter.unlimitedHeap()), "data"));
            data.setDirty();
            LogUtils.getLogger().info("Migrated saved data from {}", old);
            return data;
        } catch (IOException e) {
            LogUtils.getLogger().error("Could not read {}; it is left in place", old, e);
            return create.get();
        }
    }
    *///?}

    public static <T extends SavedData> T getOrCreate(ServerLevel level, String name,
                                                      Function<CompoundTag, T> load, Supplier<T> create) {
        //? if >=26.1 {
        /*return level.getDataStorage().computeIfAbsent(new SavedDataType<>(Dimensions.location(name),
                () -> migrate(level, name, load, create),
                CompoundTag.CODEC.xmap(load, data -> ((VersionedSavedData) data).encode()), null));
        *///?} else if >=1.20.5 {
        /*return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(create, (tag, registries) -> load.apply(tag), null), name);
        *///?} else {
        return level.getDataStorage().computeIfAbsent(load, create, name);
        //?}
    }
}
