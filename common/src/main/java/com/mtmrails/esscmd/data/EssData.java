package com.mtmrails.esscmd.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import com.mtmrails.esscmd.compat.VersionedSavedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

/**
 * All persistent mod data, saved with the overworld in {@code <world>/data/cmdessentials.dat}:
 * one {@link PlayerRecord} per player plus the server-wide warps and spawn.
 * Only touch it from the server thread.
 */
public final class EssData extends VersionedSavedData {
    private static final String NAME = "cmdessentials";
    /** 1 = P0 layout (separate maps per feature), 2 = one record per player. */
    private static final int FORMAT = 2;

    private final Map<UUID, PlayerRecord> players = new HashMap<>();
    private final Map<String, StoredLocation> warps = new TreeMap<>();
    @Nullable
    private StoredLocation spawn;

    public static EssData get(MinecraftServer server) {
        return getOrCreate(server.overworld(), NAME, EssData::load, EssData::new);
    }

    // Players

    /** The player's record, or empty if the mod has never stored anything for them. */
    public Optional<PlayerRecord> player(UUID player) {
        return Optional.ofNullable(players.get(player));
    }

    private PlayerRecord record(UUID player) {
        return players.computeIfAbsent(player, id -> new PlayerRecord());
    }

    /** Finds a known player by their last account name, ignoring case. */
    public Optional<UUID> findByName(String name) {
        for (Map.Entry<UUID, PlayerRecord> entry : players.entrySet()) {
            if (entry.getValue().lastName.equalsIgnoreCase(name)) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    public void playerJoined(UUID player, String name) {
        PlayerRecord record = record(player);
        if (!record.lastName.equals(name)) {
            record.lastName = name;
            setDirty();
        }
    }

    public void playerLeft(UUID player, String name, StoredLocation location, long time) {
        PlayerRecord record = record(player);
        record.lastName = name;
        record.lastSeen = time;
        record.logoutLocation = location;
        setDirty();
    }

    // Homes. Names are stored lower-case and sorted.

    public Map<String, StoredLocation> homes(UUID player) {
        PlayerRecord record = players.get(player);
        return record == null ? Map.of() : record.homes();
    }

    public void setHome(UUID player, String name, StoredLocation location) {
        record(player).homes.put(name, location);
        setDirty();
    }

    public boolean removeHome(UUID player, String name) {
        PlayerRecord record = players.get(player);
        if (record == null || record.homes.remove(name) == null) {
            return false;
        }
        setDirty();
        return true;
    }

    // /back

    @Nullable
    public StoredLocation back(UUID player) {
        PlayerRecord record = players.get(player);
        return record == null ? null : record.back;
    }

    public void setBack(UUID player, StoredLocation location) {
        record(player).back = location;
        setDirty();
    }

    // Teleport toggles

    public boolean teleportDisabled(UUID player) {
        PlayerRecord record = players.get(player);
        return record != null && record.teleportDisabled;
    }

    public void setTeleportDisabled(UUID player, boolean disabled) {
        PlayerRecord record = record(player);
        if (record.teleportDisabled != disabled) {
            record.teleportDisabled = disabled;
            setDirty();
        }
    }

    public boolean teleportAuto(UUID player) {
        PlayerRecord record = players.get(player);
        return record != null && record.teleportAuto;
    }

    public void setTeleportAuto(UUID player, boolean auto) {
        PlayerRecord record = record(player);
        if (record.teleportAuto != auto) {
            record.teleportAuto = auto;
            setDirty();
        }
    }

    // /fly and /god (re-applied by AbilityService)

    public boolean fly(UUID player) {
        PlayerRecord record = players.get(player);
        return record != null && record.fly;
    }

    public void setFly(UUID player, boolean fly) {
        PlayerRecord record = record(player);
        if (record.fly != fly) {
            record.fly = fly;
            setDirty();
        }
    }

    public boolean god(UUID player) {
        PlayerRecord record = players.get(player);
        return record != null && record.god;
    }

    public void setGod(UUID player, boolean god) {
        PlayerRecord record = record(player);
        if (record.god != god) {
            record.god = god;
            setDirty();
        }
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

    // Serialisation

    @Override
    protected CompoundTag write(CompoundTag tag) {
        tag.putInt("format", FORMAT);
        CompoundTag playersTag = new CompoundTag();
        players.forEach((id, record) -> playersTag.put(id.toString(), record.toTag()));
        tag.put("players", playersTag);
        tag.put("warps", writeLocations(warps));
        if (spawn != null) {
            tag.put("spawn", spawn.toTag());
        }
        return tag;
    }

    private static EssData load(CompoundTag tag) {
        EssData data = new EssData();
        if (tag.getInt("format") >= 2) {
            CompoundTag playersTag = tag.getCompound("players");
            for (String id : playersTag.getAllKeys()) {
                data.players.put(UUID.fromString(id), PlayerRecord.fromTag(playersTag.getCompound(id)));
            }
        } else {
            data.migrateFormat1(tag);
        }
        readLocations(tag.getCompound("warps"), data.warps);
        if (tag.contains("spawn")) {
            data.spawn = StoredLocation.fromTag(tag.getCompound("spawn"));
        }
        return data;
    }

    /** P0 stored homes, back and the teleport toggles as separate maps. Saved in the new layout next time. */
    private void migrateFormat1(CompoundTag tag) {
        CompoundTag homesTag = tag.getCompound("homes");
        for (String id : homesTag.getAllKeys()) {
            readLocations(homesTag.getCompound(id), record(UUID.fromString(id)).homes);
        }
        CompoundTag backTag = tag.getCompound("back");
        for (String id : backTag.getAllKeys()) {
            record(UUID.fromString(id)).back = StoredLocation.fromTag(backTag.getCompound(id));
        }
        ListTag disabled = tag.getList("teleportDisabled", Tag.TAG_STRING);
        for (int i = 0; i < disabled.size(); i++) {
            record(UUID.fromString(disabled.getString(i))).teleportDisabled = true;
        }
        ListTag auto = tag.getList("teleportAuto", Tag.TAG_STRING);
        for (int i = 0; i < auto.size(); i++) {
            record(UUID.fromString(auto.getString(i))).teleportAuto = true;
        }
        if (!players.isEmpty()) {
            setDirty();
        }
    }

    static CompoundTag writeLocations(Map<String, StoredLocation> locations) {
        CompoundTag tag = new CompoundTag();
        locations.forEach((name, location) -> tag.put(name, location.toTag()));
        return tag;
    }

    static void readLocations(CompoundTag tag, Map<String, StoredLocation> into) {
        for (String name : tag.getAllKeys()) {
            into.put(name, StoredLocation.fromTag(tag.getCompound(name)));
        }
    }
}
