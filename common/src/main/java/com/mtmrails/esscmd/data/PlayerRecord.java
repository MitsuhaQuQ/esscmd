package com.mtmrails.esscmd.data;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.ArrayList;
import com.mtmrails.esscmd.compat.Tags;
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
    /** Item id -> command run when using that item. */
    final Map<String, String> powerTools = new TreeMap<>();
    boolean powerToolsOff;
    /** Item ids refilled in hand by /unlimited. */
    final Set<String> unlimited = new TreeSet<>();
    /** Jail name, or empty when free. */
    String jail = "";
    /** Epoch millis of release; -1 = until freed. */
    long jailUntil;
    @Nullable
    StoredLocation jailReturn;
    String jailGameMode = "";
    /** Epoch millis the mute ends; 0 = not muted, -1 = until unmuted. */
    long muteUntil;
    String muteReason = "";
    /** Kit name -> epoch millis of the last claim. */
    final Map<String, Long> kitUses = new TreeMap<>();
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

    public String jail() {
        return jail;
    }

    public long jailUntil() {
        return jailUntil;
    }

    @Nullable
    public StoredLocation jailReturn() {
        return jailReturn;
    }

    public String jailGameMode() {
        return jailGameMode;
    }

    public Map<String, String> powerTools() {
        return Collections.unmodifiableMap(powerTools);
    }

    public boolean powerToolsOff() {
        return powerToolsOff;
    }

    public Set<String> unlimited() {
        return Collections.unmodifiableSet(unlimited);
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
        CompoundTag toolTag = new CompoundTag();
        powerTools.forEach(toolTag::putString);
        tag.put("powerTools", toolTag);
        tag.putBoolean("powerToolsOff", powerToolsOff);
        CompoundTag unlimitedTag = new CompoundTag();
        unlimited.forEach(id -> unlimitedTag.putBoolean(id, true));
        tag.put("unlimited", unlimitedTag);
        tag.putString("jail", jail);
        tag.putLong("jailUntil", jailUntil);
        tag.putString("jailGameMode", jailGameMode);
        if (jailReturn != null) {
            tag.put("jailReturn", jailReturn.toTag());
        }
        tag.putLong("muteUntil", muteUntil);
        tag.putString("muteReason", muteReason);
        CompoundTag kitTag = new CompoundTag();
        kitUses.forEach(kitTag::putLong);
        tag.put("kitUses", kitTag);
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
        EssData.readLocations(Tags.compound(tag, "homes"), record.homes);
        if (tag.contains("back")) {
            record.back = StoredLocation.fromTag(Tags.compound(tag, "back"));
        }
        record.teleportDisabled = Tags.bool(tag, "teleportDisabled");
        record.teleportAuto = Tags.bool(tag, "teleportAuto");
        record.lastName = Tags.string(tag, "lastName");
        record.lastSeen = Tags.longValue(tag, "lastSeen");
        if (tag.contains("logoutLocation")) {
            record.logoutLocation = StoredLocation.fromTag(Tags.compound(tag, "logoutLocation"));
        }
        record.clearConfirm = Tags.bool(tag, "clearConfirm");
        CompoundTag toolTag = Tags.compound(tag, "powerTools");
        for (String id : Tags.keys(toolTag)) {
            record.powerTools.put(id, Tags.string(toolTag, id));
        }
        record.powerToolsOff = Tags.bool(tag, "powerToolsOff");
        record.unlimited.addAll(Tags.keys(Tags.compound(tag, "unlimited")));
        record.jail = Tags.string(tag, "jail");
        record.jailUntil = Tags.longValue(tag, "jailUntil");
        record.jailGameMode = Tags.string(tag, "jailGameMode");
        if (tag.contains("jailReturn")) {
            record.jailReturn = StoredLocation.fromTag(Tags.compound(tag, "jailReturn"));
        }
        record.muteUntil = Tags.longValue(tag, "muteUntil");
        record.muteReason = Tags.string(tag, "muteReason");
        CompoundTag kitTag = Tags.compound(tag, "kitUses");
        for (String kit : Tags.keys(kitTag)) {
            record.kitUses.put(kit, Tags.longValue(kitTag, kit));
        }
        record.fly = Tags.bool(tag, "fly");
        record.god = Tags.bool(tag, "god");
        record.messagesDisabled = Tags.bool(tag, "messagesDisabled");
        record.replyToRecipient = Tags.bool(tag, "replyToRecipient");
        record.socialSpy = Tags.bool(tag, "socialSpy");
        record.nickname = Tags.string(tag, "nickname");
        ListTag ignoredTag = Tags.strings(tag, "ignored");
        for (int i = 0; i < ignoredTag.size(); i++) {
            record.ignored.add(UUID.fromString(Tags.stringAt(ignoredTag, i)));
        }
        ListTag mailTag = Tags.compounds(tag, "mail");
        for (int i = 0; i < mailTag.size(); i++) {
            record.mail.add(MailMessage.fromTag(Tags.compoundAt(mailTag, i)));
        }
        return record;
    }
}
