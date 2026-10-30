package com.mtmrails.esscmd.message;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.mtmrails.esscmd.config.ConfigManager;
import org.jetbrains.annotations.Nullable;

/** In-memory private-message reply history. It intentionally resets on server restart. */
public final class MessagingService {
    private static final Map<UUID, UUID> LAST_SENDER = new HashMap<>();
    private static final Map<UUID, UUID> LAST_RECIPIENT = new HashMap<>();
    private static final Map<UUID, Long> LAST_SENT_AT = new HashMap<>();

    private MessagingService() {}

    public static void sent(UUID sender, UUID recipient) {
        LAST_RECIPIENT.put(sender, recipient);
        LAST_SENT_AT.put(sender, System.currentTimeMillis());
        LAST_SENDER.put(recipient, sender);
    }

    @Nullable
    public static UUID replyTarget(UUID player, boolean useRecipient) {
        if (useRecipient) {
            // /rtoggle: keep talking to the last recipient until the conversation goes quiet.
            long timeout = ConfigManager.get().messaging.replyTimeoutSeconds * 1000L;
            long sentAt = LAST_SENT_AT.getOrDefault(player, 0L);
            if (LAST_RECIPIENT.containsKey(player) && System.currentTimeMillis() - sentAt < timeout) {
                return LAST_RECIPIENT.get(player);
            }
        }
        return LAST_SENDER.get(player);
    }

    public static void forget(UUID player) {
        LAST_SENDER.remove(player);
        LAST_RECIPIENT.remove(player);
        LAST_SENT_AT.remove(player);
        LAST_SENDER.values().removeIf(player::equals);
        LAST_RECIPIENT.values().removeIf(player::equals);
    }

    public static void clear() {
        LAST_SENDER.clear();
        LAST_RECIPIENT.clear();
        LAST_SENT_AT.clear();
    }
}
