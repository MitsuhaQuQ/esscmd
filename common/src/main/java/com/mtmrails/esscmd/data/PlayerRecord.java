package com.mtmrails.esscmd.data;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.ArrayList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
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
    boolean fly;
    boolean god;
    boolean messagesDisabled;
    boolean replyToRecipient;
    boolean socialSpy;
    boolean clearConfirm;
    String nickname = "";
    final Set<UUID> ignored = new HashSet<>();
    final List<MailMessage> mail = new ArrayList<>();

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

    public boolean fly() {
        return fly;
    }

    public boolean god() {
        return god;
    }

    public boolean messagesDisabled() {
        return messagesDisabled;
    }

    public boolean replyToRecipient() {
        return replyToRecipient;
    }

    public boolean socialSpy() {
        return socialSpy;
    }

    public String nickname() {
        return nickname;
    }

    public Set<UUID> ignored() {
        return Collections.unmodifiableSet(ignored);
    }

    public List<MailMessage> mail() {
        return Collections.unmodifiableList(mail);
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
        tag.putBoolean("clearConfirm", clearConfirm);
        tag.putBoolean("fly", fly);
        tag.putBoolean("god", god);
        tag.putBoolean("messagesDisabled", messagesDisabled);
        tag.putBoolean("replyToRecipient", replyToRecipient);
        tag.putBoolean("socialSpy", socialSpy);
        tag.putString("nickname", nickname);
        ListTag ignoredTag = new ListTag();
        ignored.forEach(id -> ignoredTag.add(StringTag.valueOf(id.toString())));
        tag.put("ignored", ignoredTag);
        ListTag mailTag = new ListTag();
        mail.forEach(message -> mailTag.add(message.toTag()));
        tag.put("mail", mailTag);
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
        record.clearConfirm = tag.getBoolean("clearConfirm");
        record.fly = tag.getBoolean("fly");
        record.god = tag.getBoolean("god");
        record.messagesDisabled = tag.getBoolean("messagesDisabled");
        record.replyToRecipient = tag.getBoolean("replyToRecipient");
        record.socialSpy = tag.getBoolean("socialSpy");
        record.nickname = tag.getString("nickname");
        ListTag ignoredTag = tag.getList("ignored", Tag.TAG_STRING);
        for (int i = 0; i < ignoredTag.size(); i++) {
            record.ignored.add(UUID.fromString(ignoredTag.getString(i)));
        }
        ListTag mailTag = tag.getList("mail", Tag.TAG_COMPOUND);
        for (int i = 0; i < mailTag.size(); i++) {
            record.mail.add(MailMessage.fromTag(mailTag.getCompound(i)));
        }
        return record;
    }
}
