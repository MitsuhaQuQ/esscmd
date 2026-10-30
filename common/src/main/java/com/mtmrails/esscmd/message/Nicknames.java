package com.mtmrails.esscmd.message;

import com.mtmrails.esscmd.CmdEssentials;
import com.mtmrails.esscmd.config.ConfigManager;
import com.mtmrails.esscmd.config.EssConfig;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
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
        String name = nickname.isEmpty() ? player.getGameProfile().getName()
                : ConfigManager.get().nick.prefix + nickname;
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

    /** Checks length, {@code nick.allowedRegex} and {@code nick.blacklist}. Formatting codes aren't supported. */
    public static boolean valid(String nickname) {
        EssConfig.Nick config = ConfigManager.get().nick;
        if (nickname.isEmpty() || nickname.length() > config.maxLength) {
            return false;
        }
        try {
            if (!Pattern.compile(config.allowedRegex).matcher(nickname).find()) {
                return false;
            }
            for (String banned : config.blacklist) {
                if (Pattern.compile(banned, Pattern.CASE_INSENSITIVE).matcher(nickname).find()) {
                    return false;
                }
            }
        } catch (PatternSyntaxException e) {
            CmdEssentials.LOGGER.warn("Invalid regular expression in the nick config: {}", e.getMessage());
            return false;
        }
        return true;
    }
}
