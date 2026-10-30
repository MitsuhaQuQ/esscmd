package io.github.cmdessentials.permission;

import java.util.List;

/** Every permission node of the mod. Both loaders register exactly this list. */
public final class PermissionNodes {
    public static final PermissionNode ESSENTIALS = new PermissionNode("cmdessentials.essentials", 0);
    public static final PermissionNode ESSENTIALS_RELOAD = new PermissionNode("cmdessentials.essentials.reload", 2);

    public static final List<PermissionNode> ALL = List.of(ESSENTIALS, ESSENTIALS_RELOAD);

    private PermissionNodes() {}
}
