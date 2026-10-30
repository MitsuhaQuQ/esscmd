package com.mtmrails.esscmd.compat;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
//? if >=1.19.3 {
import net.minecraft.core.registries.BuiltInRegistries;
//?} else {
/*import net.minecraft.core.Registry;
*///?}

/** The item and block registries, which moved in 1.19.3. */
public final class ItemLookup {
    private ItemLookup() {}

    public static Optional<Item> find(ResourceLocation id) {
        //? if >=1.19.3 {
        return BuiltInRegistries.ITEM.getOptional(id);
        //?} else {
        /*return Registry.ITEM.getOptional(id);
        *///?}
    }

    /** The namespaced id, such as {@code minecraft:diamond}. */
    public static ResourceLocation id(Item item) {
        //? if >=1.19.3 {
        return BuiltInRegistries.ITEM.getKey(item);
        //?} else {
        /*return Registry.ITEM.getKey(item);
        *///?}
    }

    public static Optional<EntityType<?>> entity(ResourceLocation id) {
        //? if >=1.19.3 {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(id);
        //?} else {
        /*return Registry.ENTITY_TYPE.getOptional(id);
        *///?}
    }

    public static Iterable<ResourceLocation> entityIds() {
        //? if >=1.19.3 {
        return BuiltInRegistries.ENTITY_TYPE.keySet();
        //?} else {
        /*return Registry.ENTITY_TYPE.keySet();
        *///?}
    }

    /** The namespaced block id, such as {@code minecraft:lava}. */
    public static ResourceLocation id(Block block) {
        //? if >=1.19.3 {
        return BuiltInRegistries.BLOCK.getKey(block);
        //?} else {
        /*return Registry.BLOCK.getKey(block);
        *///?}
    }

    public static Iterable<ResourceLocation> ids() {
        //? if >=1.19.3 {
        return BuiltInRegistries.ITEM.keySet();
        //?} else {
        /*return Registry.ITEM.keySet();
        *///?}
    }
}
