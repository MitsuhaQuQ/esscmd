package com.mtmrails.esscmd.compat;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignBlockEntity;
//? if >=26.1 {
/*import net.minecraft.world.level.block.entity.SignTextSlot;
*///?}

/** Sign text; signs got a separate {@code SignText} per side in 1.20. */
public final class Signs {
    private Signs() {}

    /** Sets a line (0-3) on the front. */
    public static void setLine(SignBlockEntity sign, int line, Component text) {
        //? if >=26.1 {
        /*sign.updateText(current -> current.asMutable().setLine(line, text).asImmutable(), SignTextSlot.FRONT);
        *///?} else if >=1.20 {
        sign.updateText(current -> current.setMessage(line, text), true);
        //?} else {
        /*sign.setMessage(line, text);
        *///?}
    }
}
