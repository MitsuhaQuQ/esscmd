package com.mtmrails.esscmd.config;

/**
 * {@code config/cmdessentials/config.json}. Gson fills missing fields from these defaults, and
 * the file is rewritten on load so new options show up for admins.
 */
public final class EssConfig {
    /**
     * Default language: used for the console and for players whose client language has no
     * {@code config/cmdessentials/messages_<language>.json}.
     */
    public String language = "en_us";
    /** Show each player messages in their own client language when a messages file exists for it. */
    public boolean perPlayerLanguage = true;
    public Teleport teleport = new Teleport();
    public Homes homes = new Homes();
    public Spawn spawn = new Spawn();
    public Back back = new Back();
    public Tpa tpa = new Tpa();
    public Speed speed = new Speed();
    public Afk afk = new Afk();
    public Near near = new Near();
    public Motd motd = new Motd();

    public static final class Teleport {
        /** Seconds to wait before a player's own teleport happens. 0 teleports at once. */
        public int delaySeconds = 3;
        /** Cancel a delayed teleport when the player moves. */
        public boolean cancelOnMove = true;
        /** Seconds between two teleports of the same player. */
        public int cooldownSeconds = 0;
    }

    public static final class Homes {
        /** Homes per player unless the {@code cmdessentials.sethome.limit} option says otherwise. -1 = unlimited. */
        public int defaultLimit = 3;
    }

    public static final class Spawn {
        /** After death, respawn at /setspawn when the player has no bed or respawn anchor. */
        public boolean respawnAtSpawn = true;
        /** Send players to /setspawn when they join for the first time. */
        public boolean firstJoinAtSpawn = true;
    }

    public static final class Back {
        /** /back also returns to the place where the player died. */
        public boolean saveOnDeath = true;
    }

    public static final class Tpa {
        /** Seconds before an unanswered /tpa request expires. */
        public int timeoutSeconds = 120;
    }

    public static final class Afk {
        /** Seconds without moving, chatting or commands before a player is marked AFK. 0 = never. */
        public int autoSeconds = 300;
        /** Seconds without activity before an AFK player is kicked. 0 = never. */
        public int kickSeconds = 0;
        /** Announce AFK changes to everyone; otherwise only the player is told. */
        public boolean broadcast = true;
    }

    public static final class Near {
        /** Radius for /near without a radius. */
        public int defaultRadius = 200;
        /** Larger requested radii are capped to this. */
        public int maxRadius = 1000;
    }

    public static final class Motd {
        /** Show motd_<lang>.txt (its intro, before the first #chapter) when a player joins. */
        public boolean showOnJoin = true;
    }

    public static final class Speed {
        /** Walking speed at /speed 10, as a multiple of the vanilla speed. */
        public double maxWalkMultiplier = 5.0;
        /** Flying speed at /speed 10, as a multiple of the vanilla speed. */
        public double maxFlyMultiplier = 10.0;
    }
}
