package com.mtmrails.esscmd.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mtmrails.esscmd.compat.MovementSpeed;
import com.mtmrails.esscmd.compat.Players;
import com.mtmrails.esscmd.message.Messages;
import com.mtmrails.esscmd.permission.PermissionNodes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * {@code /speed [walk|fly] <multiplier> [player]}. Without a type, flying players get fly speed and
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
        return Commands.argument("speed", FloatArgumentType.floatArg(0))
                .executes(ctx -> speed(ctx, type, ctx.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                        .requires(PermissionNodes.SPEED_OTHERS.require())
                        .executes(ctx -> speed(ctx, type, EntityArgument.getPlayer(ctx, "player"))));
    }

    private static int speed(CommandContext<CommandSourceStack> ctx, @Nullable Type type, ServerPlayer target)
            throws CommandSyntaxException {
        float speed = FloatArgumentType.getFloat(ctx, "speed");
        Type effective = type != null ? type : target.getAbilities().flying ? Type.FLY : Type.WALK;
        if (effective == Type.FLY) {
            target.getAbilities().setFlyingSpeed(VANILLA_FLY_SPEED * speed);
            target.onUpdateAbilities();
        } else {
            MovementSpeed.setWalkMultiplier(target, speed);
        }
        String typeName = Messages.raw(ctx.getSource(), effective == Type.FLY ? "speed.fly" : "speed.walk");
        return CommandUtil.ok(ctx.getSource(), "speed.set", typeName, speed, Players.name(target));
    }
}
