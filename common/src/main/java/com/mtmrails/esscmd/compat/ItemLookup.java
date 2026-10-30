package com.mtmrails.esscmd.compat;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
//? if >=1.19.3 {
import net.minecraft.core.registries.BuiltInRegistries;
//?} else {
/*import net.minecraft.core.Registry;
*///?}

/** The item registry, which moved in 1.19.3. */
public final class ItemLookup {
    private ItemLookup() {}

    public static Optional<Item> find(ResourceLocation id) {
        //? if >=1.19.3 {
        return BuiltInRegistries.ITEM.getOptional(id);
        //?} else {
        /*return Registry.ITEM.getOptional(id);
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
