package com.mtmrails.esscmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mtmrails.esscmd.compat.MovementSpeed;
import com.mtmrails.esscmd.config.ConfigManager;
import com.mtmrails.esscmd.config.EssConfig;
import com.mtmrails.esscmd.message.Messages;
import com.mtmrails.esscmd.permission.PermissionNodes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * {@code /speed [walk|fly] <0-10> [player]}. Without a type, flying players get fly speed and
 * everyone else walk speed. 1 is the vanilla speed. The change lasts until relog or respawn.
 */
public final class SpeedCommand {
    private static final float VANILLA_FLY_SPEED = 0.05f;

    private enum Type { WALK, FLY }

    private SpeedCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("speed")
                .requires(PermissionNodes.SPEED.require())
                .then(Commands.literal("walk").then(speedArgument(Type.WALK)))
                .then(Commands.literal("fly").then(speedArgument(Type.FLY)))
                .then(speedArgument(null)));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, Float> speedArgument(@Nullable Type type) {
        return Commands.argument("speed", FloatArgumentType.floatArg(0, 10))
                .executes(ctx -> speed(ctx, type, ctx.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                        .requires(PermissionNodes.SPEED_OTHERS.require())
                        .executes(ctx -> speed(ctx, type, EntityArgument.getPlayer(ctx, "player"))));
    }

    private static int speed(CommandContext<CommandSourceStack> ctx, @Nullable Type type, ServerPlayer target)
            throws CommandSyntaxException {
        float speed = FloatArgumentType.getFloat(ctx, "speed");
        Type effective = type != null ? type : target.getAbilities().flying ? Type.FLY : Type.WALK;
        EssConfig.Speed config = ConfigManager.get().speed;
        if (effective == Type.FLY) {
            target.getAbilities().setFlyingSpeed(VANILLA_FLY_SPEED * (float) multiplier(speed, config.maxFlyMultiplier));
            target.onUpdateAbilities();
        } else {
            MovementSpeed.setWalkMultiplier(target, multiplier(speed, config.maxWalkMultiplier));
        }
        String typeName = Messages.raw(ctx.getSource(), effective == Type.FLY ? "speed.fly" : "speed.walk");
        return CommandUtil.ok(ctx.getSource(), "speed.set", typeName, speed, target.getGameProfile().getName());
    }

    /** 0..1 scales the vanilla speed linearly; 1..10 goes linearly from vanilla to {@code max} times vanilla. */
    static double multiplier(float speed, double max) {
        if (speed <= 1) {
            return speed;
        }
        return 1 + (speed - 1) / 9.0 * (max - 1);
    }
}
