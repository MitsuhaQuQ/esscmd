package com.mtmrails.esscmd.compat;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
//? if <1.19 {
/*import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
*///?}

/** Text and feedback helpers whose vanilla API differs between versions. */
public final class Texts {
    private Texts() {}

    public static MutableComponent literal(String text) {
        //? if >=1.19 {
        return Component.literal(text);
        //?} else {
        /*return new TextComponent(text);
        *///?}
    }

    /** A vanilla translation key, which every client can render. Never use it for our own text. */
    public static MutableComponent vanillaTranslatable(String key) {
        //? if >=1.19 {
        return Component.translatable(key);
        //?} else {
        /*return new TranslatableComponent(key);
        *///?}
    }

    /** Literal text with {@code &} colour codes, which clients still render as {@code §} codes. */
    public static MutableComponent colored(String text) {
        return literal(text.replaceAll("&([0-9a-fk-orA-FK-OR])", "\u00a7$1"));
    }

    public static MutableComponent empty() {
        return literal("");
    }

    public static void success(CommandSourceStack source, Component message, boolean broadcastToOps) {
        //? if >=1.20 {
        source.sendSuccess(() -> message, broadcastToOps);
        //?} else {
        /*source.sendSuccess(message, broadcastToOps);
        *///?}
    }

    public static void failure(CommandSourceStack source, Component message) {
        source.sendFailure(message);
    }

    /** Makes {@code text} run {@code command} when clicked in chat. */
    public static MutableComponent runOnClick(MutableComponent text, String command, Component hover) {
        return text.withStyle(style -> style
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
    }
}
