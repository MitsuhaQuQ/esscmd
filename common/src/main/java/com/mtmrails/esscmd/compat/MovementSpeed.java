package com.mtmrails.esscmd.compat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
//? if >=1.20.5 {
/*import com.mtmrails.esscmd.CmdEssentials;
import net.minecraft.resources.ResourceLocation;
*///?} else {
import java.util.UUID;
//?}

/** Walking-speed modifier; attribute modifiers are keyed by UUID before 1.20.5 and by id after. */
public final class MovementSpeed {
    //? if >=1.20.5 {
    /*private static final ResourceLocation WALK_SPEED_ID = Dimensions.location(CmdEssentials.MOD_ID, "walk_speed");
    *///?} else {
    private static final UUID WALK_SPEED_ID = UUID.fromString("5f0e6b8e-3f6c-4a8e-9d1e-2c7b6f0a9e41");
    //?}

    private MovementSpeed() {}

    /** Multiplies the player's walking speed; {@code 1} removes our modifier. Not persisted. */
    public static void setWalkMultiplier(ServerPlayer player, double multiplier) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        speed.removeModifier(WALK_SPEED_ID);
        if (multiplier == 1.0) {
            return;
        }
        //? if >=1.20.5 {
        /*speed.addTransientModifier(new AttributeModifier(WALK_SPEED_ID, multiplier - 1.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        *///?} else {
        speed.addTransientModifier(new AttributeModifier(WALK_SPEED_ID, "cmdessentials walk speed", multiplier - 1.0,
                AttributeModifier.Operation.MULTIPLY_TOTAL));
        //?}
    }
}
