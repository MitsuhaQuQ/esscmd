package com.mtmrails.esscmd.command;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import com.mtmrails.esscmd.compat.Players;
import com.mtmrails.esscmd.compat.Texts;
import com.mtmrails.esscmd.message.Messages;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/** Small helpers shared by the command classes. */
final class CommandUtil {
    private static final Pattern NAME = Pattern.compile("[a-z0-9_-]{1,32}");

    private CommandUtil() {}

    static int ok(CommandSourceStack source, String key, Object... args) {
        Texts.success(source, Messages.info(source, key, args), false);
        return 1;
    }

    static int fail(CommandSourceStack source, String key, Object... args) {
        Texts.failure(source, Messages.error(source, key, args));
        return 0;
    }

    /** Lower-cased home or warp name, or null (after telling the source) if it isn't valid. */
    @Nullable
    static String validName(CommandSourceStack source, String raw) {
        String name = raw.toLowerCase(Locale.ROOT);
        if (!NAME.matcher(name).matches()) {
            fail(source, "error.invalid-name", raw);
            return null;
        }
        return name;
    }

    static void tell(ServerPlayer player, String key, Object... args) {
        Players.send(player, Messages.info(player, key, args));
    }

    /** Owner of a {@code [player:]name} argument: the named player for admins, else the source player. */
    record OwnedName(UUID owner, String ownerName, boolean other, String name) {}

    /**
     * Parses {@code name} or {@code player:name}. Returns null after telling the source when the
     * player is unknown. {@code name} may be empty ({@code "player:"}).
     */
    @Nullable
    static OwnedName ownedName(CommandSourceStack source, ServerPlayer self, String input) {
        int colon = input.indexOf(':');
        if (colon < 0) {
            return new OwnedName(self.getUUID(), self.getGameProfile().getName(), false, input);
        }
        String playerName = input.substring(0, colon);
        Optional<UUID> id = Players.findId(source.getServer(), playerName);
        if (id.isEmpty()) {
            fail(source, "error.player-not-found", playerName);
            return null;
        }
        boolean other = !id.get().equals(self.getUUID());
        return new OwnedName(id.get(), playerName, other, input.substring(colon + 1));
    }
}
