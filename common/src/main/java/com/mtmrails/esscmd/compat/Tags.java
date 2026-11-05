package com.mtmrails.esscmd.compat;

import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/** NBT readers: getters return Optionals from 1.21.5, and missing keys read as empty here. */
public final class Tags {
    private Tags() {}

    public static String string(CompoundTag tag, String key) {
        //? if >=1.21.5 {
        /*return tag.getStringOr(key, "");
        *///?} else {
        return tag.getString(key);
        //?}
    }

    public static boolean bool(CompoundTag tag, String key) {
        //? if >=1.21.5 {
        /*return tag.getBooleanOr(key, false);
        *///?} else {
        return tag.getBoolean(key);
        //?}
    }

    public static int integer(CompoundTag tag, String key) {
        //? if >=1.21.5 {
        /*return tag.getIntOr(key, 0);
        *///?} else {
        return tag.getInt(key);
        //?}
    }

    public static long longValue(CompoundTag tag, String key) {
        //? if >=1.21.5 {
        /*return tag.getLongOr(key, 0L);
        *///?} else {
        return tag.getLong(key);
        //?}
    }

    public static double doubleValue(CompoundTag tag, String key) {
        //? if >=1.21.5 {
        /*return tag.getDoubleOr(key, 0.0);
        *///?} else {
        return tag.getDouble(key);
        //?}
    }

    public static float floatValue(CompoundTag tag, String key) {
        //? if >=1.21.5 {
        /*return tag.getFloatOr(key, 0.0F);
        *///?} else {
        return tag.getFloat(key);
        //?}
    }

    public static CompoundTag compound(CompoundTag tag, String key) {
        //? if >=1.21.5 {
        /*return tag.getCompoundOrEmpty(key);
        *///?} else {
        return tag.getCompound(key);
        //?}
    }

    /** A list of compound tags. */
    public static ListTag compounds(CompoundTag tag, String key) {
        //? if >=1.21.5 {
        /*return tag.getListOrEmpty(key);
        *///?} else {
        return tag.getList(key, net.minecraft.nbt.Tag.TAG_COMPOUND);
        //?}
    }

    /** A list of strings. */
    public static ListTag strings(CompoundTag tag, String key) {
        //? if >=1.21.5 {
        /*return tag.getListOrEmpty(key);
        *///?} else {
        return tag.getList(key, net.minecraft.nbt.Tag.TAG_STRING);
        //?}
    }

    public static CompoundTag compoundAt(ListTag list, int index) {
        //? if >=1.21.5 {
        /*return list.getCompoundOrEmpty(index);
        *///?} else {
        return list.getCompound(index);
        //?}
    }

    public static String stringAt(ListTag list, int index) {
        //? if >=1.21.5 {
        /*return list.getStringOr(index, "");
        *///?} else {
        return list.getString(index);
        //?}
    }

    public static Set<String> keys(CompoundTag tag) {
        //? if >=1.21.5 {
        /*return tag.keySet();
        *///?} else {
        return tag.getAllKeys();
        //?}
    }
}
