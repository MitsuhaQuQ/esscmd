package com.mtmrails.esscmd.permission;

import java.util.List;
import com.mtmrails.esscmd.config.ConfigManager;

/** Every permission node and option of the mod. Both loaders register exactly these lists. */
public final class PermissionNodes {
    private static final int ALL_PLAYERS = 0;
    private static final int ADMINS = 2;

    public static final PermissionNode ESSENTIALS = node("essentials", ALL_PLAYERS);
    public static final PermissionNode ESSENTIALS_RELOAD = node("essentials.reload", ADMINS);

    public static final PermissionNode SETHOME = node("sethome", ALL_PLAYERS);
    public static final PermissionNode HOME = node("home", ALL_PLAYERS);
    public static final PermissionNode DELHOME = node("delhome", ALL_PLAYERS);
    public static final PermissionNode RENAMEHOME = node("renamehome", ALL_PLAYERS);
    /** Lets the {@code player:name} form of the home commands manage other players' homes. */
    public static final PermissionNode HOME_OTHERS = node("home.others", ADMINS);

    public static final PermissionNode SETSPAWN = node("setspawn", ADMINS);
    public static final PermissionNode SPAWN = node("spawn", ALL_PLAYERS);
    public static final PermissionNode SPAWN_OTHERS = node("spawn.others", ADMINS);

    public static final PermissionNode SPEED = node("speed", ADMINS);
    public static final PermissionNode SPEED_OTHERS = node("speed.others", ADMINS);

    public static final PermissionNode BACK = node("back", ALL_PLAYERS);
    public static final PermissionNode BACK_OTHERS = node("back.others", ADMINS);

    public static final PermissionNode TPA = node("tpa", ALL_PLAYERS);
    public static final PermissionNode TPAHERE = node("tpahere", ALL_PLAYERS);
    public static final PermissionNode TPACCEPT = node("tpaccept", ALL_PLAYERS);
    public static final PermissionNode TPDENY = node("tpdeny", ALL_PLAYERS);
    public static final PermissionNode TPACANCEL = node("tpacancel", ALL_PLAYERS);
    public static final PermissionNode TPTOGGLE = node("tptoggle", ALL_PLAYERS);
    public static final PermissionNode TPTOGGLE_OTHERS = node("tptoggle.others", ADMINS);
    public static final PermissionNode TPAUTO = node("tpauto", ALL_PLAYERS);
    public static final PermissionNode TPAUTO_OTHERS = node("tpauto.others", ADMINS);

    public static final PermissionNode WARP = node("warp", ALL_PLAYERS);
    public static final PermissionNode WARP_OTHERS = node("warp.others", ADMINS);
    public static final PermissionNode WARPINFO = node("warpinfo", ALL_PLAYERS);
    public static final PermissionNode SETWARP = node("setwarp", ADMINS);
    public static final PermissionNode DELWARP = node("delwarp", ADMINS);

    public static final PermissionNode TELEPORT_BYPASS_DELAY = node("teleport.bypass.delay", ADMINS);
    public static final PermissionNode TELEPORT_BYPASS_COOLDOWN = node("teleport.bypass.cooldown", ADMINS);

    public static final List<PermissionNode> ALL = List.of(
            ESSENTIALS, ESSENTIALS_RELOAD,
            SETHOME, HOME, DELHOME, RENAMEHOME, HOME_OTHERS,
            SETSPAWN, SPAWN, SPAWN_OTHERS,
            SPEED, SPEED_OTHERS,
            BACK, BACK_OTHERS,
            TPA, TPAHERE, TPACCEPT, TPDENY, TPACANCEL, TPTOGGLE, TPTOGGLE_OTHERS, TPAUTO, TPAUTO_OTHERS,
            WARP, WARP_OTHERS, WARPINFO, SETWARP, DELWARP,
            TELEPORT_BYPASS_DELAY, TELEPORT_BYPASS_COOLDOWN);

    public static final IntOption HOME_LIMIT =
            new IntOption("cmdessentials.sethome.limit", () -> ConfigManager.get().homes.defaultLimit);

    public static final List<IntOption> INT_OPTIONS = List.of(HOME_LIMIT);

    private PermissionNodes() {}

    private static PermissionNode node(String name, int opLevel) {
        return new PermissionNode("cmdessentials." + name, opLevel);
    }
}
