package io.github.cmdessentials.permission;

import java.util.function.Predicate;
import io.github.cmdessentials.platform.Platform;
import net.minecraft.commands.CommandSourceStack;

/** A permission node with the OP level used when no permission provider is installed. */
public record PermissionNode(String name, int opLevel) {
    public boolean check(CommandSourceStack source) {
        return Platform.get().hasPermission(source, name, opLevel);
    }

    /** For Brigadier's {@code requires(...)}. */
    public Predicate<CommandSourceStack> require() {
        return this::check;
    }
}
