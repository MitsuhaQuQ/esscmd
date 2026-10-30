package com.mtmrails.esscmd.permission;

import java.util.List;
import com.mtmrails.esscmd.config.ConfigManager;

/** Every permission node and option of the mod. Both loaders register exactly these lists. */
public final class PermissionNodes {
    private static final int ALL_PLAYERS = 0;
    private static final int ADMINS = 2;
    /** Running commands as someone else is as powerful as the console. */
    private static final int OWNERS = 4;

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

    public static final PermissionNode FLY = node("fly", ADMINS);
    public static final PermissionNode FLY_OTHERS = node("fly.others", ADMINS);
    public static final PermissionNode GOD = node("god", ADMINS);
    public static final PermissionNode GOD_OTHERS = node("god.others", ADMINS);
    public static final PermissionNode HEAL = node("heal", ADMINS);
    public static final PermissionNode HEAL_OTHERS = node("heal.others", ADMINS);
    public static final PermissionNode FEED = node("feed", ADMINS);
    public static final PermissionNode FEED_OTHERS = node("feed.others", ADMINS);
    public static final PermissionNode REST = node("rest", ADMINS);
    public static final PermissionNode REST_OTHERS = node("rest.others", ADMINS);
    public static final PermissionNode EXT = node("ext", ADMINS);
    public static final PermissionNode EXT_OTHERS = node("ext.others", ADMINS);
    public static final PermissionNode SUICIDE = node("suicide", ALL_PLAYERS);
    public static final PermissionNode AFK = node("afk", ALL_PLAYERS);
    public static final PermissionNode AFK_OTHERS = node("afk.others", ADMINS);
    /** Never kicked for being AFK (afk.kickSeconds). */
    public static final PermissionNode AFK_KICK_EXEMPT = node("afk.kickexempt", ADMINS);

    public static final PermissionNode GETPOS = node("getpos", ALL_PLAYERS);
    public static final PermissionNode GETPOS_OTHERS = node("getpos.others", ADMINS);
    public static final PermissionNode COMPASS = node("compass", ALL_PLAYERS);
    public static final PermissionNode DEPTH = node("depth", ALL_PLAYERS);
    public static final PermissionNode NEAR = node("near", ADMINS);
    public static final PermissionNode NEAR_OTHERS = node("near.others", ADMINS);
    public static final PermissionNode SEEN = node("seen", ALL_PLAYERS);
    public static final PermissionNode WHOIS = node("whois", ADMINS);
    /** Shows the IP address in /whois. */
    public static final PermissionNode WHOIS_IP = node("whois.ip", ADMINS);
    public static final PermissionNode PLAYTIME = node("playtime", ALL_PLAYERS);
    public static final PermissionNode PLAYTIME_OTHERS = node("playtime.others", ALL_PLAYERS);
    public static final PermissionNode PING = node("ping", ALL_PLAYERS);
    public static final PermissionNode MOTD = node("motd", ALL_PLAYERS);
    public static final PermissionNode RULES = node("rules", ALL_PLAYERS);
    public static final PermissionNode INFO = node("info", ALL_PLAYERS);

    public static final PermissionNode TPHERE = node("tphere", ADMINS);
    public static final PermissionNode TPOHERE = node("tpohere", ADMINS);
    public static final PermissionNode TPO = node("tpo", ADMINS);
    public static final PermissionNode TPPOS = node("tppos", ADMINS);
    public static final PermissionNode TPALL = node("tpall", ADMINS);
    public static final PermissionNode TPAALL = node("tpaall", ADMINS);
    public static final PermissionNode TPOFFLINE = node("tpoffline", ADMINS);
    public static final PermissionNode TOP = node("top", ADMINS);
    public static final PermissionNode BOTTOM = node("bottom", ADMINS);
    public static final PermissionNode JUMP = node("jump", ADMINS);
    public static final PermissionNode TPR = node("tpr", ALL_PLAYERS);
    public static final PermissionNode SETTPR = node("settpr", ADMINS);
    public static final PermissionNode WORLD = node("world", ADMINS);

    public static final PermissionNode MSG = node("msg", ALL_PLAYERS);
    public static final PermissionNode REPLY = node("reply", ALL_PLAYERS);
    public static final PermissionNode MSGTOGGLE = node("msgtoggle", ALL_PLAYERS);
    public static final PermissionNode MSGTOGGLE_OTHERS = node("msgtoggle.others", ADMINS);
    public static final PermissionNode RTOGGLE = node("rtoggle", ALL_PLAYERS);
    public static final PermissionNode RTOGGLE_OTHERS = node("rtoggle.others", ADMINS);
    public static final PermissionNode IGNORE = node("ignore", ALL_PLAYERS);
    public static final PermissionNode SOCIALSPY = node("socialspy", ADMINS);
    public static final PermissionNode SOCIALSPY_OTHERS = node("socialspy.others", ADMINS);
    public static final PermissionNode HELPOP = node("helpop", ALL_PLAYERS);
    public static final PermissionNode HELPOP_RECEIVE = node("helpop.receive", ADMINS);
    public static final PermissionNode MAIL = node("mail", ALL_PLAYERS);
    public static final PermissionNode MAIL_OTHERS = node("mail.others", ADMINS);
    public static final PermissionNode MAIL_SENDALL = node("mail.sendall", ADMINS);
    public static final PermissionNode BROADCAST = node("broadcast", ADMINS);
    public static final PermissionNode BROADCASTWORLD = node("broadcastworld", ADMINS);
    public static final PermissionNode NICK = node("nick", ADMINS);
    public static final PermissionNode NICK_OTHERS = node("nick.others", ADMINS);
    public static final PermissionNode REALNAME = node("realname", ALL_PLAYERS);

    public static final PermissionNode ENDERCHEST = node("enderchest", ADMINS);
    public static final PermissionNode ENDERCHEST_OTHERS = node("enderchest.others", ADMINS);
    public static final PermissionNode INVSEE = node("invsee", ADMINS);
    public static final PermissionNode INVSEE_MODIFY = node("invsee.modify", ADMINS);
    public static final PermissionNode HAT = node("hat", ADMINS);
    public static final PermissionNode REPAIR = node("repair", ADMINS);
    public static final PermissionNode REPAIR_ALL = node("repair.all", ADMINS);
    public static final PermissionNode MORE = node("more", ADMINS);
    public static final PermissionNode CLEARINVENTORY = node("clearinventory", ADMINS);
    public static final PermissionNode CLEARINVENTORY_OTHERS = node("clearinventory.others", ADMINS);
    public static final PermissionNode DISPOSAL = node("disposal", ALL_PLAYERS);
    public static final PermissionNode WORKBENCH = node("workbench", ADMINS);
    public static final PermissionNode ANVIL = node("anvil", ADMINS);
    public static final PermissionNode CARTOGRAPHYTABLE = node("cartographytable", ADMINS);
    public static final PermissionNode GRINDSTONE = node("grindstone", ADMINS);
    public static final PermissionNode LOOM = node("loom", ADMINS);
    public static final PermissionNode SMITHINGTABLE = node("smithingtable", ADMINS);
    public static final PermissionNode STONECUTTER = node("stonecutter", ADMINS);

    public static final PermissionNode KIT = node("kit", ALL_PLAYERS);
    public static final PermissionNode KIT_OTHERS = node("kit.others", ADMINS);
    public static final PermissionNode SHOWKIT = node("showkit", ALL_PLAYERS);
    public static final PermissionNode CREATEKIT = node("createkit", ADMINS);
    public static final PermissionNode DELKIT = node("delkit", ADMINS);
    public static final PermissionNode KITRESET = node("kitreset", ADMINS);

    public static final PermissionNode MUTE = node("mute", ADMINS);
    public static final PermissionNode MUTE_EXEMPT = node("mute.exempt", ADMINS);
    public static final PermissionNode TEMPBAN = node("tempban", ADMINS);
    public static final PermissionNode TEMPBAN_EXEMPT = node("tempban.exempt", ADMINS);
    public static final PermissionNode TEMPBANIP = node("tempbanip", ADMINS);
    public static final PermissionNode UNBAN = node("unban", ADMINS);
    public static final PermissionNode UNBANIP = node("unbanip", ADMINS);
    public static final PermissionNode KICKALL = node("kickall", ADMINS);
    public static final PermissionNode SUDO = node("sudo", OWNERS);
    public static final PermissionNode SUDO_EXEMPT = node("sudo.exempt", OWNERS);
    public static final PermissionNode GC = node("gc", ADMINS);

    public static final PermissionNode HEAL_COOLDOWN_BYPASS = node("heal.cooldown.bypass", ADMINS);
    public static final PermissionNode MUTE_UNLIMITED = node("mute.unlimited", ADMINS);
    public static final PermissionNode TEMPBAN_UNLIMITED = node("tempban.unlimited", ADMINS);

    public static final PermissionNode SETJAIL = node("setjail", ADMINS);
    public static final PermissionNode DELJAIL = node("deljail", ADMINS);
    public static final PermissionNode JAILS = node("jails", ADMINS);
    public static final PermissionNode JAILEDPLAYERS = node("jailedplayers", ADMINS);
    public static final PermissionNode TOGGLEJAIL = node("togglejail", ADMINS);
    public static final PermissionNode JAIL_EXEMPT = node("jail.exempt", ADMINS);
    public static final PermissionNode VANISH = node("vanish", ADMINS);
    public static final PermissionNode VANISH_OTHERS = node("vanish.others", ADMINS);
    public static final PermissionNode VANISH_SEE = node("vanish.see", ADMINS);

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
            FLY, FLY_OTHERS, GOD, GOD_OTHERS, HEAL, HEAL_OTHERS, FEED, FEED_OTHERS,
            REST, REST_OTHERS, EXT, EXT_OTHERS, SUICIDE, AFK, AFK_OTHERS, AFK_KICK_EXEMPT,
            GETPOS, GETPOS_OTHERS, COMPASS, DEPTH, NEAR, NEAR_OTHERS, SEEN, WHOIS, WHOIS_IP,
            PLAYTIME, PLAYTIME_OTHERS, PING, MOTD, RULES, INFO,
            TPHERE, TPOHERE, TPO, TPPOS, TPALL, TPAALL, TPOFFLINE, TOP, BOTTOM, JUMP, TPR, SETTPR, WORLD,
            MSG, REPLY, MSGTOGGLE, MSGTOGGLE_OTHERS, RTOGGLE, RTOGGLE_OTHERS, IGNORE,
            SOCIALSPY, SOCIALSPY_OTHERS, HELPOP, HELPOP_RECEIVE, MAIL, MAIL_OTHERS, MAIL_SENDALL,
            BROADCAST, BROADCASTWORLD, NICK, NICK_OTHERS, REALNAME,
            ENDERCHEST, ENDERCHEST_OTHERS, INVSEE, INVSEE_MODIFY, HAT, REPAIR, REPAIR_ALL, MORE, CLEARINVENTORY, CLEARINVENTORY_OTHERS, DISPOSAL, WORKBENCH, ANVIL, CARTOGRAPHYTABLE, GRINDSTONE, LOOM, SMITHINGTABLE, STONECUTTER,
            KIT, KIT_OTHERS, SHOWKIT, CREATEKIT, DELKIT, KITRESET,
            MUTE, MUTE_EXEMPT, TEMPBAN, TEMPBAN_EXEMPT, TEMPBANIP, UNBAN, UNBANIP, KICKALL, SUDO, SUDO_EXEMPT, GC,
            HEAL_COOLDOWN_BYPASS, MUTE_UNLIMITED, TEMPBAN_UNLIMITED,
            SETJAIL, DELJAIL, JAILS, JAILEDPLAYERS, TOGGLEJAIL, JAIL_EXEMPT, VANISH, VANISH_OTHERS, VANISH_SEE,
            TELEPORT_BYPASS_DELAY, TELEPORT_BYPASS_COOLDOWN);

    public static final IntOption HOME_LIMIT =
            new IntOption("cmdessentials.sethome.limit", () -> ConfigManager.get().homes.defaultLimit);

    public static final List<IntOption> INT_OPTIONS = List.of(HOME_LIMIT);

    private PermissionNodes() {}

    private static PermissionNode node(String name, int opLevel) {
        return new PermissionNode("cmdessentials." + name, opLevel);
    }
}
