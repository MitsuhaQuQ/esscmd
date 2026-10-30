package com.mtmrails.esscmd.data;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

/**
 * Everything the mod stores about one player. Only {@link EssData} changes it, so every change
 * marks the data dirty. Add new per-player state here rather than as another map in EssData.
 */
public final class PlayerRecord {
    /** Lower-case name, sorted. */
    final Map<String, StoredLocation> homes = new TreeMap<>();
    @Nullable
    StoredLocation back;
    boolean teleportDisabled;
    boolean teleportAuto;
    /** Last known account name, for offline lookups and messages. */
    String lastName = "";
    /** Epoch millis of the last logout; 0 if never seen leaving. */
    long lastSeen;
    @Nullable
    StoredLocation logoutLocation;

    PlayerRecord() {}

    public Map<String, StoredLocation> homes() {
        return Collections.unmodifiableMap(homes);
    }

    @Nullable
    public StoredLocation back() {
        return back;
    }

    public boolean teleportDisabled() {
        return teleportDisabled;
    }

    public boolean teleportAuto() {
        return teleportAuto;
    }

    public String lastName() {
        return lastName;
    }

    public long lastSeen() {
        return lastSeen;
    }

    @Nullable
    public StoredLocation logoutLocation() {
        return logoutLocation;
    }

    CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.put("homes", EssData.writeLocations(homes));
        if (back != null) {
            tag.put("back", back.toTag());
        }
        tag.putBoolean("teleportDisabled", teleportDisabled);
        tag.putBoolean("teleportAuto", teleportAuto);
        tag.putString("lastName", lastName);
        tag.putLong("lastSeen", lastSeen);
        if (logoutLocation != null) {
            tag.put("logoutLocation", logoutLocation.toTag());
        }
        return tag;
    }

    static PlayerRecord fromTag(CompoundTag tag) {
        PlayerRecord record = new PlayerRecord();
        EssData.readLocations(tag.getCompound("homes"), record.homes);
        if (tag.contains("back")) {
            record.back = StoredLocation.fromTag(tag.getCompound("back"));
        }
        record.teleportDisabled = tag.getBoolean("teleportDisabled");
        record.teleportAuto = tag.getBoolean("teleportAuto");
        record.lastName = tag.getString("lastName");
        record.lastSeen = tag.getLong("lastSeen");
        if (tag.contains("logoutLocation")) {
            record.logoutLocation = StoredLocation.fromTag(tag.getCompound("logoutLocation"));
        }
        return record;
    }
}
