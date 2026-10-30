package com.mtmrails.esscmd.data;

import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.Set;
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
    private final Map<String, Kit> kits = new TreeMap<>();
    private final Map<String, StoredLocation> jails = new TreeMap<>();
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

    public Set<UUID> knownPlayers() {
        return Collections.unmodifiableSet(players.keySet());
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

    // Messaging

    public boolean messagesDisabled(UUID player) {
        PlayerRecord record = players.get(player);
        return record != null && record.messagesDisabled;
    }

    public void setMessagesDisabled(UUID player, boolean disabled) {
        PlayerRecord record = record(player);
        if (record.messagesDisabled != disabled) {
            record.messagesDisabled = disabled;
            setDirty();
        }
    }

    public boolean replyToRecipient(UUID player) {
        PlayerRecord record = players.get(player);
        return record != null && record.replyToRecipient;
    }

    public void setReplyToRecipient(UUID player, boolean enabled) {
        PlayerRecord record = record(player);
        if (record.replyToRecipient != enabled) {
            record.replyToRecipient = enabled;
            setDirty();
        }
    }

    // Jail

    public Map<String, StoredLocation> jails() {
        return Collections.unmodifiableMap(jails);
    }

    public void setJail(String name, StoredLocation location) {
        jails.put(name, location);
        setDirty();
    }

    public boolean removeJail(String name) {
        boolean removed = jails.remove(name) != null;
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public void setJailed(UUID player, String jail, long until, StoredLocation returnTo, String gameMode) {
        PlayerRecord record = record(player);
        record.jail = jail;
        record.jailUntil = until;
        record.jailReturn = returnTo;
        record.jailGameMode = gameMode;
        setDirty();
    }

    public void clearJailed(UUID player) {
        PlayerRecord record = record(player);
        record.jail = "";
        record.jailUntil = 0;
        record.jailReturn = null;
        record.jailGameMode = "";
        setDirty();
    }

    /** Names of everyone in jail, sorted. */
    public List<String> jailedNames() {
        return players.values().stream().filter(record -> !record.jail.isEmpty()).map(PlayerRecord::lastName).sorted().toList();
    }

    // /mute

    public boolean muted(UUID player) {
        PlayerRecord record = players.get(player);
        if (record == null || record.muteUntil == 0) {
            return false;
        }
        if (record.muteUntil > 0 && System.currentTimeMillis() >= record.muteUntil) {
            clearMute(player);
            return false;
        }
        return true;
    }

    public void setMute(UUID player, long until, String reason) {
        PlayerRecord record = record(player);
        record.muteUntil = until;
        record.muteReason = reason;
        setDirty();
    }

    public void clearMute(UUID player) {
        PlayerRecord record = players.get(player);
        if (record != null) {
            record.muteUntil = 0;
            record.muteReason = "";
            setDirty();
        }
    }

    // Kits

    public Map<String, Kit> kits() {
        return Collections.unmodifiableMap(kits);
    }

    public void setKit(String name, Kit kit) {
        kits.put(name, kit);
        setDirty();
    }

    /** Also forgets every player's cooldown for it, so a new kit of that name starts fresh. */
    public boolean removeKit(String name) {
        if (kits.remove(name) == null) {
            return false;
        }
        players.values().forEach(record -> record.kitUses.remove(name));
        setDirty();
        return true;
    }

    /** Epoch millis of the player's last claim, or 0. */
    public long kitUsed(UUID player, String kit) {
        PlayerRecord record = players.get(player);
        return record == null ? 0 : record.kitUses.getOrDefault(kit, 0L);
    }

    public void setKitUsed(UUID player, String kit, long time) {
        record(player).kitUses.put(kit, time);
        setDirty();
    }

    public void resetKit(UUID player, String kit) {
        PlayerRecord record = players.get(player);
        if (record != null && record.kitUses.remove(kit) != null) {
            setDirty();
        }
    }

    public boolean clearConfirm(UUID player) {
        PlayerRecord record = players.get(player);
        return record != null && record.clearConfirm;
    }

    public void setClearConfirm(UUID player, boolean confirm) {
        record(player).clearConfirm = confirm;
        setDirty();
    }

    public boolean socialSpy(UUID player) {
        PlayerRecord record = players.get(player);
        return record != null && record.socialSpy;
    }

    public void setSocialSpy(UUID player, boolean enabled) {
        PlayerRecord record = record(player);
        if (record.socialSpy != enabled) {
            record.socialSpy = enabled;
            setDirty();
        }
    }

    public boolean ignores(UUID player, UUID ignored) {
        PlayerRecord record = players.get(player);
        return record != null && record.ignored.contains(ignored);
    }

    /** Returns true when the player is ignored after the toggle. */
    public boolean toggleIgnored(UUID player, UUID ignored) {
        PlayerRecord record = record(player);
        boolean value;
        if (record.ignored.remove(ignored)) {
            value = false;
        } else {
            record.ignored.add(ignored);
            value = true;
        }
        setDirty();
        return value;
    }

    public String nickname(UUID player) {
        PlayerRecord record = players.get(player);
        return record == null ? "" : record.nickname;
    }

    public void setNickname(UUID player, String nickname) {
        PlayerRecord record = record(player);
        if (!record.nickname.equals(nickname)) {
            record.nickname = nickname;
            setDirty();
        }
    }

    public Optional<UUID> findByNickname(String nickname) {
        for (Map.Entry<UUID, PlayerRecord> entry : players.entrySet()) {
            if (!entry.getValue().nickname.isEmpty() && entry.getValue().nickname.equalsIgnoreCase(nickname)) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    public void addMail(UUID player, MailMessage message) {
        record(player).mail.add(message);
        setDirty();
    }

    /** Removes expired mail, then returns a snapshot. */
    public List<MailMessage> mail(UUID player, long now) {
        PlayerRecord record = players.get(player);
        if (record == null) {
            return List.of();
        }
        if (record.mail.removeIf(message -> message.expired(now))) {
            setDirty();
        }
        return List.copyOf(record.mail);
    }

    public int unreadMail(UUID player, long now) {
        return (int) mail(player, now).stream().filter(message -> !message.read()).count();
    }

    public void markMailRead(UUID player) {
        PlayerRecord record = players.get(player);
        if (record == null) {
            return;
        }
        boolean changed = false;
        for (int i = 0; i < record.mail.size(); i++) {
            MailMessage message = record.mail.get(i);
            if (!message.read()) {
                record.mail.set(i, message.withRead(true));
                changed = true;
            }
        }
        if (changed) {
            setDirty();
        }
    }

    public boolean removeMail(UUID player, int number) {
        PlayerRecord record = players.get(player);
        if (record == null || number < 1 || number > record.mail.size()) {
            return false;
        }
        record.mail.remove(number - 1);
        setDirty();
        return true;
    }

    public int clearMail(UUID player) {
        PlayerRecord record = players.get(player);
        if (record == null) {
            return 0;
        }
        int count = record.mail.size();
        if (count > 0) {
            record.mail.clear();
            setDirty();
        }
        return count;
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
        tag.put("jails", writeLocations(jails));
        CompoundTag kitsTag = new CompoundTag();
        kits.forEach((name, kit) -> kitsTag.put(name, kit.toTag()));
        tag.put("kits", kitsTag);
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
        readLocations(tag.getCompound("jails"), data.jails);
        CompoundTag kitsTag = tag.getCompound("kits");
        for (String name : kitsTag.getAllKeys()) {
            data.kits.put(name, Kit.fromTag(kitsTag.getCompound(name)));
        }
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
