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
    /** Vanilla commands replaced by cmdEssentials. /emsg is always available. */
    public java.util.List<String> overrideVanilla = new java.util.ArrayList<>(java.util.List.of("msg", "tell", "w"));
    public Teleport teleport = new Teleport();
    public Homes homes = new Homes();
    public Spawn spawn = new Spawn();
    public Back back = new Back();
    public Tpa tpa = new Tpa();
    public Speed speed = new Speed();
    public Afk afk = new Afk();
    public Near near = new Near();
    public Motd motd = new Motd();
    public Tpr tpr = new Tpr();
    public Heal heal = new Heal();
    public Nick nick = new Nick();
    public Messaging messaging = new Messaging();
    public Mail mail = new Mail();
    public Moderation moderation = new Moderation();
    public God god = new God();
    public Warps warps = new Warps();
    public Kits kits = new Kits();
    public Items items = new Items();
    public Jail jail = new Jail();
    /** spawnmob-limit: most mobs per /spawnmob. */
    public int spawnmobLimit = 10;
    /** Commands this mod shouldn't register (Essentials' disabled-commands). Needs a restart. */
    public java.util.List<String> disabledCommands = new java.util.ArrayList<>();

    public static final class Teleport {
        /** Seconds to wait before a player's own teleport happens. 0 teleports at once. */
        public int delaySeconds = 3;
        /** Cancel a delayed teleport when the player moves. */
        public boolean cancelOnMove = true;
        /** Seconds between two teleports of the same player. */
        public int cooldownSeconds = 0;
        /** is-water-safe: water counts as a safe place to land. */
        public boolean waterIsSafe = false;
        /** teleport-to-center: land in the middle of the block (safe-spot teleports). */
        public boolean toCenter = true;
        /** Standard block ids nobody should be teleported onto or into (/top, /tpr, /jump, ...). */
        public java.util.List<String> unsafeBlocks = new java.util.ArrayList<>(java.util.List.of(
                "minecraft:lava", "minecraft:magma_block", "minecraft:cactus", "minecraft:fire",
                "minecraft:soul_fire", "minecraft:campfire", "minecraft:soul_campfire",
                "minecraft:sweet_berry_bush", "minecraft:powder_snow", "minecraft:wither_rose",
                "minecraft:pointed_dripstone"));
    }

    public static final class Homes {
        /** Homes per player unless the {@code cmdessentials.sethome.limit} option says otherwise. -1 = unlimited. */
        public int defaultLimit = 3;
        /** spawn-if-no-home: /home goes to the spawn when the player has no home. */
        public boolean spawnIfNoHome = true;
        /** confirm-home-overwrite: /sethome on an existing name must be run twice. */
        public boolean confirmOverwrite = false;
    }

    public static final class Spawn {
        /** After death, respawn at /setspawn when the player has no bed or respawn anchor. */
        public boolean respawnAtSpawn = true;
        /** Send players to /setspawn when they join for the first time. */
        public boolean firstJoinAtSpawn = true;
        /** respawn-at-home: after death, respawn at the player's home if they have one. */
        public boolean respawnAtHome = false;
        /** respawn-at-home-bed: a bed or respawn anchor wins over respawnAtHome. */
        public boolean respawnAtHomeBed = true;
    }

    public static final class Back {
        /** /back also returns to the place where the player died. */
        public boolean saveOnDeath = true;
    }

    public static final class Tpa {
        /** Seconds before an unanswered /tpa request expires. */
        public int timeoutSeconds = 120;
        /** tpa-max-requests: pending requests per target; the oldest is dropped beyond this. */
        public int maxRequests = 5;
    }

    public static final class Afk {
        /** Seconds without moving, chatting or commands before a player is marked AFK. 0 = never. */
        public int autoSeconds = 300;
        /** Seconds without activity before an AFK player is kicked. 0 = never. */
        public int kickSeconds = 0;
        /** Announce AFK changes to everyone; otherwise only the player is told. */
        public boolean broadcast = true;
        /** cancel-afk-on-move: moving or turning ends AFK. */
        public boolean cancelOnMove = true;
        /** cancel-afk-on-chat: chatting or running a command ends AFK. */
        public boolean cancelOnChat = true;
    }

    public static final class Heal {
        /** heal-cooldown: seconds between /heal uses on yourself. */
        public int cooldownSeconds = 60;
        /** remove-effects-on-heal. */
        public boolean removeEffects = true;
    }

    public static final class Nick {
        /** nickname-prefix: shown before nicknames, so they can't pass as real names. */
        public String prefix = "~";
        /** max-nick-length: not counting the prefix. */
        public int maxLength = 16;
        /** allowed-nicks-regex. */
        public String allowedRegex = "^[a-zA-Z_0-9]+$";
        /** nick-blacklist: regular expressions, matched anywhere, ignoring case. */
        public java.util.List<String> blacklist = new java.util.ArrayList<>();
    }

    public static final class Messaging {
        /** last-message-reply-recipient-timeout: with /rtoggle on, /r follows the last recipient for this long. */
        public int replyTimeoutSeconds = 180;
    }

    public static final class Mail {
        /** mails-per-minute: per sender. */
        public int perMinute = 1000;
    }

    public static final class Moderation {
        /** max-mute-time: longest /mute in seconds without mute.unlimited. -1 = no limit. */
        public long maxMuteSeconds = -1;
        /** max-tempban-time: longest /tempban in seconds without tempban.unlimited. -1 = no limit. */
        public long maxTempbanSeconds = -1;
    }

    public static final class God {
        /** remove-god-on-disconnect. */
        public boolean removeOnDisconnect = false;
        /** no-god-in-worlds: dimension ids where /god has no effect. */
        public java.util.List<String> disabledDimensions = new java.util.ArrayList<>();
    }

    public static final class Warps {
        /** per-warp-permission: each warp needs cmdessentials.warps.<name>. */
        public boolean perWarpPermission = false;
    }

    public static final class Kits {
        /** drop-items-if-full: drop what doesn't fit; otherwise refuse the kit. */
        public boolean dropItemsIfFull = false;
        /** skip-used-one-time-kits-from-kit-list. */
        public boolean skipUsedOneTimeKits = false;
    }

    public static final class Items {
        /** default-stack-size: /more without an amount; -1 = full stack. */
        public int defaultStackSize = -1;
        /** repair-enchanted: /repair may fix enchanted items. */
        public boolean repairEnchanted = true;
    }

    public static final class Jail {
        /** Blocks a jailed player may walk from the jail point before being pulled back. */
        public int radius = 8;
        /** teleport-when-freed: back, spawn or off. */
        public String teleportWhenFreed = "back";
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

    public static final class Tpr {
        /** Spots tried per /tpr; each may load or generate a chunk. */
        public int attempts = 10;
        /** Dimension id -> area. /tpr is only allowed in dimensions listed here; /settpr edits it. */
        public java.util.Map<String, TprArea> areas = new java.util.LinkedHashMap<>(java.util.Map.of(
                "minecraft:overworld", new TprArea(0, 0, 200, 5000),
                "minecraft:the_nether", new TprArea(0, 0, 50, 1000)));
    }

    public static final class TprArea {
        public int centerX;
        public int centerZ;
        public int minRange;
        public int maxRange;

        public TprArea() {}

        public TprArea(int centerX, int centerZ, int minRange, int maxRange) {
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.minRange = minRange;
            this.maxRange = maxRange;
        }
    }

    public static final class Speed {
        /** Walking speed at /speed 10, as a multiple of the vanilla speed. */
        public double maxWalkMultiplier = 4.0;
        /** Flying speed at /speed 10, as a multiple of the vanilla speed. */
        public double maxFlyMultiplier = 8.0;
    }
}
