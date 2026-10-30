package com.mtmrails.esscmd.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if >=1.20.5 {
/*import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;
*///?} else {
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
//?}

/** Item stacks to and from NBT: plain tags before 1.20.5, registry-aware data components after. */
public final class ItemData {
    private ItemData() {}

    /** Sets the custom name, or removes it when {@code name} is null. */
    public static void setName(ItemStack stack, Component name) {
        //? if >=1.20.5 {
        /*if (name == null) {
            stack.remove(DataComponents.CUSTOM_NAME);
        } else {
            stack.set(DataComponents.CUSTOM_NAME, name);
        }
        *///?} else {
        if (name == null) {
            stack.resetHoverName();
        } else {
            stack.setHoverName(name);
        }
        //?}
    }

    public static List<Component> lore(ItemStack stack) {
        //? if >=1.20.5 {
        /*return new ArrayList<>(stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY).lines());
        *///?} else {
        List<Component> lines = new ArrayList<>();
        CompoundTag display = stack.getTagElement("display");
        if (display != null) {
            ListTag list = display.getList("Lore", 8);
            for (int i = 0; i < list.size(); i++) {
                lines.add(Component.Serializer.fromJson(list.getString(i)));
            }
        }
        return lines;
        //?}
    }

    public static void setLore(ItemStack stack, List<Component> lines) {
        //? if >=1.20.5 {
        /*stack.set(DataComponents.LORE, new ItemLore(lines));
        *///?} else {
        ListTag list = new ListTag();
        lines.forEach(line -> list.add(StringTag.valueOf(Component.Serializer.toJson(line))));
        stack.getOrCreateTagElement("display").put("Lore", list);
        //?}
    }

    /** A player head showing {@code owner}'s skin. */
    public static ItemStack playerHead(String owner) {
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        //? if >=1.20.5 {
        /*head.set(DataComponents.PROFILE, new ResolvableProfile(Optional.of(owner), Optional.empty(), new PropertyMap()));
        *///?} else {
        head.getOrCreateTag().putString("SkullOwner", owner);
        //?}
        return head;
    }

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
