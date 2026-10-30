package com.mtmrails.esscmd.permission;

import java.util.function.Predicate;
import com.mtmrails.esscmd.platform.Platform;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

/** A permission node with the OP level used when no permission provider is installed. */
public record PermissionNode(String name, int opLevel) {
    public boolean check(CommandSourceStack source) {
        return Platform.get().hasPermission(source, name, opLevel);
    }

    public boolean check(ServerPlayer player) {
        return check(player.createCommandSourceStack());
    }

    /** For Brigadier's {@code requires(...)}. */
    public Predicate<CommandSourceStack> require() {
        return this::check;
    }
}
