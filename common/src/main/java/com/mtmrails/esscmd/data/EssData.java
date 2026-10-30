package com.mtmrails.esscmd.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import com.mtmrails.esscmd.compat.VersionedSavedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

/**
 * All persistent mod data, saved with the overworld in {@code <world>/data/cmdessentials.dat}.
 * Only touch it from the server thread.
 */
public final class EssData extends VersionedSavedData {
    private static final String NAME = "cmdessentials";

    private final Map<UUID, Map<String, StoredLocation>> homes = new HashMap<>();
    private final Map<String, StoredLocation> warps = new TreeMap<>();
    private final Map<UUID, StoredLocation> back = new HashMap<>();
    private final Set<UUID> teleportDisabled = new HashSet<>();
    private final Set<UUID> teleportAuto = new HashSet<>();
    @Nullable
    private StoredLocation spawn;

    public static EssData get(MinecraftServer server) {
        return getOrCreate(server.overworld(), NAME, EssData::load, EssData::new);
    }

    // Homes. Names are stored lower-case and sorted.

    public Map<String, StoredLocation> homes(UUID player) {
        return Collections.unmodifiableMap(homes.getOrDefault(player, Map.of()));
    }

    public void setHome(UUID player, String name, StoredLocation location) {
        homes.computeIfAbsent(player, id -> new TreeMap<>()).put(name, location);
        setDirty();
    }

    public boolean removeHome(UUID player, String name) {
        Map<String, StoredLocation> own = homes.get(player);
        if (own == null || own.remove(name) == null) {
            return false;
        }
        if (own.isEmpty()) {
            homes.remove(player);
        }
        setDirty();
        return true;
    }

    // Warps

    public Map<String, StoredLocation> warps() {
        return Collections.unmodifiableMap(warps);
    }

    public void setWarp(String name, StoredLocation location) {
        warps.put(name, location);
        setDirty();
    }

    public boolean removeWarp(String name) {
        boolean removed = warps.remove(name) != null;
        if (removed) {
            setDirty();
        }
        return removed;
    }

    // Spawn

    @Nullable
    public StoredLocation spawn() {
        return spawn;
    }

    public void setSpawn(StoredLocation location) {
        spawn = location;
        setDirty();
    }

    // /back

    @Nullable
    public StoredLocation back(UUID player) {
        return back.get(player);
    }

    public void setBack(UUID player, StoredLocation location) {
        back.put(player, location);
        setDirty();
    }

    // Teleport toggles

    public boolean teleportDisabled(UUID player) {
        return teleportDisabled.contains(player);
    }

    public void setTeleportDisabled(UUID player, boolean disabled) {
        if (disabled ? teleportDisabled.add(player) : teleportDisabled.remove(player)) {
            setDirty();
        }
    }

    public boolean teleportAuto(UUID player) {
        return teleportAuto.contains(player);
    }

    public void setTeleportAuto(UUID player, boolean auto) {
        if (auto ? teleportAuto.add(player) : teleportAuto.remove(player)) {
            setDirty();
        }
    }

    // Serialisation

    @Override
    protected CompoundTag write(CompoundTag tag) {
        CompoundTag homesTag = new CompoundTag();
        homes.forEach((player, own) -> homesTag.put(player.toString(), writeLocations(own)));
        tag.put("homes", homesTag);
        tag.put("warps", writeLocations(warps));
        CompoundTag backTag = new CompoundTag();
        back.forEach((player, location) -> backTag.put(player.toString(), location.toTag()));
        tag.put("back", backTag);
        tag.put("teleportDisabled", writeIds(teleportDisabled));
        tag.put("teleportAuto", writeIds(teleportAuto));
        if (spawn != null) {
            tag.put("spawn", spawn.toTag());
        }
        return tag;
    }

    private static EssData load(CompoundTag tag) {
        EssData data = new EssData();
        CompoundTag homesTag = tag.getCompound("homes");
        for (String player : homesTag.getAllKeys()) {
            Map<String, StoredLocation> own = new TreeMap<>();
            readLocations(homesTag.getCompound(player), own);
            data.homes.put(UUID.fromString(player), own);
        }
        readLocations(tag.getCompound("warps"), data.warps);
        CompoundTag backTag = tag.getCompound("back");
        for (String player : backTag.getAllKeys()) {
            data.back.put(UUID.fromString(player), StoredLocation.fromTag(backTag.getCompound(player)));
        }
        readIds(tag.getList("teleportDisabled", Tag.TAG_STRING), data.teleportDisabled);
        readIds(tag.getList("teleportAuto", Tag.TAG_STRING), data.teleportAuto);
        if (tag.contains("spawn")) {
            data.spawn = StoredLocation.fromTag(tag.getCompound("spawn"));
        }
        return data;
    }

    private static CompoundTag writeLocations(Map<String, StoredLocation> locations) {
        CompoundTag tag = new CompoundTag();
        locations.forEach((name, location) -> tag.put(name, location.toTag()));
        return tag;
    }

    private static void readLocations(CompoundTag tag, Map<String, StoredLocation> into) {
        for (String name : tag.getAllKeys()) {
            into.put(name, StoredLocation.fromTag(tag.getCompound(name)));
        }
    }

    private static ListTag writeIds(Set<UUID> ids) {
        ListTag list = new ListTag();
        ids.forEach(id -> list.add(StringTag.valueOf(id.toString())));
        return list;
    }

    private static void readIds(ListTag list, Set<UUID> into) {
        for (int i = 0; i < list.size(); i++) {
            into.add(UUID.fromString(list.getString(i)));
        }
    }
}
