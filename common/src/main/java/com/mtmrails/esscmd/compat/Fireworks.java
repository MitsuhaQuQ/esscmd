package com.mtmrails.esscmd.compat;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
//? if >=1.20.5 {
/*import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.FireworkExplosion;
*///?} else {
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
//?}

/** Firework rocket flight power and explosions: NBT before 1.20.5, the {@code fireworks} component after. */
public final class Fireworks {
    public static final int MAX_EXPLOSIONS = 256;

    private Fireworks() {}

    /**
     * One explosion. {@code shape}: 0 small ball, 1 large ball, 2 star, 3 creeper, 4 burst.
     * Colours are RGB values, as in {@code DyeColor.getFireworkColor()}.
     */
    public record Explosion(int shape, List<Integer> colors, List<Integer> fadeColors, boolean trail, boolean twinkle) {}

    public static int power(ItemStack stack) {
        //? if >=1.20.5 {
        /*var fireworks = stack.get(DataComponents.FIREWORKS);
        return fireworks == null ? 1 : fireworks.flightDuration();
        *///?} else {
        CompoundTag tag = stack.getTagElement("Fireworks");
        return tag == null ? 1 : tag.getByte("Flight");
        //?}
    }

    public static List<Explosion> explosions(ItemStack stack) {
        List<Explosion> result = new ArrayList<>();
        //? if >=1.20.5 {
        /*var fireworks = stack.get(DataComponents.FIREWORKS);
        if (fireworks != null) {
            for (FireworkExplosion e : fireworks.explosions()) {
                result.add(new Explosion(e.shape().getId(), List.copyOf(e.colors()), List.copyOf(e.fadeColors()),
                        e.hasTrail(), e.hasTwinkle()));
            }
        }
        *///?} else {
        CompoundTag tag = stack.getTagElement("Fireworks");
        if (tag != null) {
            ListTag list = tag.getList("Explosions", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag e = list.getCompound(i);
                result.add(new Explosion(e.getByte("Type"), ints(e.getIntArray("Colors")), ints(e.getIntArray("FadeColors")),
                        e.getBoolean("Trail"), e.getBoolean("Flicker")));
            }
        }
        //?}
        return result;
    }

    public static void set(ItemStack stack, int power, List<Explosion> explosions) {
        //? if >=1.20.5 {
        /*List<FireworkExplosion> list = explosions.stream()
                .map(e -> new FireworkExplosion(FireworkExplosion.Shape.byId(e.shape()), new IntArrayList(e.colors()),
                        new IntArrayList(e.fadeColors()), e.trail(), e.twinkle()))
                .toList();
        stack.set(DataComponents.FIREWORKS, new net.minecraft.world.item.component.Fireworks(power, list));
        *///?} else {
        CompoundTag tag = stack.getOrCreateTagElement("Fireworks");
        tag.putByte("Flight", (byte) power);
        ListTag list = new ListTag();
        for (Explosion e : explosions) {
            CompoundTag entry = new CompoundTag();
            entry.putByte("Type", (byte) e.shape());
            entry.putIntArray("Colors", e.colors());
            entry.putIntArray("FadeColors", e.fadeColors());
            entry.putBoolean("Trail", e.trail());
            entry.putBoolean("Flicker", e.twinkle());
            list.add(entry);
        }
        tag.put("Explosions", list);
        //?}
    }

    /** Launches one rocket made from {@code stack} at the given position. */
    public static void launch(ServerLevel level, double x, double y, double z, ItemStack stack) {
        ItemStack one = stack.copy();
        one.setCount(1);
        level.addFreshEntity(new FireworkRocketEntity(level, x, y, z, one));
    }

    //? if <1.20.5 {
    private static List<Integer> ints(int[] values) {
        List<Integer> list = new ArrayList<>(values.length);
        for (int value : values) {
            list.add(value);
        }
        return list;
    }
    //?}
}
