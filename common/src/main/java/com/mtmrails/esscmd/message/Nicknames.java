package com.mtmrails.esscmd.message;

import java.util.Locale;
import com.mtmrails.esscmd.compat.Texts;
import com.mtmrails.esscmd.data.EssData;
import com.mtmrails.esscmd.player.AfkService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/** Nickname display shared by the display-name and tab-list mixins. */
public final class Nicknames {
    private Nicknames() {}

    public static MutableComponent displayName(ServerPlayer player) {
        String nickname = EssData.get(player.server).nickname(player.getUUID());
        String name = nickname.isEmpty() ? player.getGameProfile().getName() : nickname;
        MutableComponent display = Texts.literal(name);
        if (!nickname.isEmpty()) {
            display.withStyle(ChatFormatting.GOLD);
        }
        return display;
    }

    public static MutableComponent tabListName(ServerPlayer player) {
        MutableComponent display = Texts.empty();
        if (AfkService.isAfk(player)) {
            display.append(Texts.literal("[AFK] ").withStyle(ChatFormatting.GRAY));
        }
        return display.append(displayName(player));
    }

    /** Plain nickname accepted by vanilla clients; formatting codes are deliberately unsupported. */
    public static boolean valid(String nickname) {
        if (nickname.length() < 1 || nickname.length() > 32) {
            return false;
        }
        String lower = nickname.toLowerCase(Locale.ROOT);
        return lower.chars().allMatch(c -> Character.isLetterOrDigit(c) || c == '_' || c == '-');
    }
}
