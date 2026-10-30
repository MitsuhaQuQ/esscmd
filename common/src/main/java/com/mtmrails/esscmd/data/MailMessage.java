package com.mtmrails.esscmd.data;

import net.minecraft.nbt.CompoundTag;

/** One persisted /mail message. An expiry of 0 means that it never expires. */
public record MailMessage(String senderName, String text, long sentAt, long expiresAt, boolean read) {
    public boolean expired(long now) {
        return expiresAt > 0 && now >= expiresAt;
    }

    MailMessage withRead(boolean value) {
        return new MailMessage(senderName, text, sentAt, expiresAt, value);
    }

    CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("sender", senderName);
        tag.putString("text", text);
        tag.putLong("sentAt", sentAt);
        tag.putLong("expiresAt", expiresAt);
        tag.putBoolean("read", read);
        return tag;
    }

    static MailMessage fromTag(CompoundTag tag) {
        return new MailMessage(tag.getString("sender"), tag.getString("text"), tag.getLong("sentAt"),
                tag.getLong("expiresAt"), tag.getBoolean("read"));
    }
}
