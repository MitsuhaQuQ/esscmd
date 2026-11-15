package com.mtmrails.esscmd.compat.mixin;

import java.util.Collection;
import com.mtmrails.esscmd.teleport.TeleportService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.server.commands.TeleportCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla {@code /tp} and {@code /teleport} (and {@code /etp}) save the /back location of every
 * player they move, like Essentials' own /tp. Both entry points get the targets before anyone moves.
 * {@code LookAt} is package-private before 26.1, hence {@code @Coerce Object}.
 */
@Mixin(TeleportCommand.class)
public abstract class TeleportCommandMixin {
    @Inject(method = "teleportToEntity", at = @At("HEAD"))
    private static void cmdessentials$toEntity(CommandSourceStack source, Collection<? extends Entity> targets,
                                               Entity destination, CallbackInfoReturnable<Integer> cir) {
        cmdessentials$recordBack(targets);
    }

    @Inject(method = "teleportToPos", at = @At("HEAD"))
    private static void cmdessentials$toPos(CommandSourceStack source, Collection<? extends Entity> targets,
                                            ServerLevel level, Coordinates position, Coordinates rotation,
                                            @Coerce Object lookAt, CallbackInfoReturnable<Integer> cir) {
        cmdessentials$recordBack(targets);
    }

    private static void cmdessentials$recordBack(Collection<? extends Entity> targets) {
        for (Entity entity : targets) {
            if (entity instanceof ServerPlayer player) {
                TeleportService.recordBack(player);
            }
        }
    }
}
