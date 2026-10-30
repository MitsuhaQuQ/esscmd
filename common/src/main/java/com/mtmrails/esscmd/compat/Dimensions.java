package com.mtmrails.esscmd.compat;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
//? if >=1.19.3 {
import net.minecraft.core.registries.Registries;
//?} else {
/*import net.minecraft.core.Registry;
*///?}

/** Dimension and resource-location helpers whose vanilla API differs between versions. */
public final class Dimensions {
    private Dimensions() {}

    public static String id(ResourceKey<Level> dimension) {
        return dimension.location().toString();
    }

    public static ResourceKey<Level> key(String id) {
        //? if >=1.19.3 {
        return ResourceKey.create(Registries.DIMENSION, location(id));
        //?} else {
        /*return ResourceKey.create(Registry.DIMENSION_REGISTRY, location(id));
        *///?}
    }

    public static ResourceLocation location(String id) {
        //? if >=1.21 {
        /*return ResourceLocation.parse(id);
        *///?} else {
        return new ResourceLocation(id);
        //?}
    }

    public static ResourceLocation location(String namespace, String path) {
        //? if >=1.21 {
        /*return ResourceLocation.fromNamespaceAndPath(namespace, path);
        *///?} else {
        return new ResourceLocation(namespace, path);
        //?}
    }
}
