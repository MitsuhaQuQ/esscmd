package com.mtmrails.esscmd.data;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * A kit. {@code delaySeconds}: 0 = no cooldown, negative = once per player. {@code opLevel} is
 * stored here rather than in a permission node, because Forge can't register nodes after
 * startup. Items are serialised tags: decoding needs registries, which EssData.load lacks.
 */
public record Kit(long delaySeconds, int opLevel, List<CompoundTag> items) {
    CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("delay", delaySeconds);
        tag.putInt("opLevel", opLevel);
        ListTag list = new ListTag();
        items.forEach(item -> list.add(item.copy()));
        tag.put("items", list);
        return tag;
    }

    static Kit fromTag(CompoundTag tag) {
        ListTag list = tag.getList("items", Tag.TAG_COMPOUND);
        List<CompoundTag> items = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            items.add(list.getCompound(i));
        }
        return new Kit(tag.getLong("delay"), tag.getInt("opLevel"), List.copyOf(items));
    }
}
