package com.mtmrails.esscmd.compat;

import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
//? if >=1.20.5 {
/*import net.minecraft.nbt.Tag;
*///?}

/** Item stacks to and from NBT: plain tags before 1.20.5, registry-aware data components after. */
public final class ItemData {
    private ItemData() {}

    /** {@code stack} must not be empty. */
    public static CompoundTag save(MinecraftServer server, ItemStack stack) {
        //? if >=1.20.5 {
        /*Tag tag = stack.save(server.registryAccess());
        return (CompoundTag) tag;
        *///?} else {
        return stack.save(new CompoundTag());
        //?}
    }

    /** Empty if the item no longer exists, e.g. it came from a removed mod. */
    public static Optional<ItemStack> load(MinecraftServer server, CompoundTag tag) {
        //? if >=1.20.5 {
        /*return ItemStack.parse(server.registryAccess(), tag);
        *///?} else {
        ItemStack stack = ItemStack.of(tag);
        return stack.isEmpty() ? Optional.empty() : Optional.of(stack);
        //?}
    }
}
