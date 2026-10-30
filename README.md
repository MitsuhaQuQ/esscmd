# esscmd (cmdEssentials)

[![Build](https://github.com/MitsuhaQuQ/esscmd/actions/workflows/build.yml/badge.svg)](https://github.com/MitsuhaQuQ/esscmd/actions/workflows/build.yml)

A **server-only** Minecraft mod that brings the commands of the Bukkit
[EssentialsX](https://essentialsx.net/commands) plugin to Fabric and Forge servers.

Install it on the server only. Players join with an unmodified vanilla client.

> **Status: early development.** All of P0 and P1 is implemented: homes, spawn, warps, `/back`,
> the `/tpa` family, teleport extensions, player state, AFK, info commands, messaging, mail,
> nicknames, inventory tools, portable workbenches and kits. P2 has started with moderation.
> See the [roadmap](#roadmap). Core features have been played in-game on five of the six
> targets; Forge 1.21.1 has only passed server-console tests so far.

## Supported versions

| Minecraft | Fabric | Forge | Java |
|-----------|:------:|:-----:|:----:|
| 1.18.2    | ✅     | ✅    | 17   |
| 1.20.1    | ✅     | ✅    | 17   |
| 1.21.1    | ✅     | ✅    | 21   |

NeoForge is not supported yet.

## Installation

1. Download the jar that matches your loader and Minecraft version, for example
   `cmdessentials-fabric-0.1.0-alpha+1.20.1.jar` or `cmdessentials-forge-0.1.0-alpha+1.21.1.jar`.
2. Put it in the server's `mods/` folder.
3. **Fabric only:** also install [Fabric API](https://modrinth.com/mod/fabric-api) from
   Modrinth. For 1.18.2, the aggregate jars on the Fabric Maven are empty stubs.
4. Start the server.

The mod doesn't load in singleplayer or on clients. That is intentional.

## Commands

Syntax follows [EssentialsX](https://essentialsx.net/commands). "Admins" means OP level 2
unless a permission plugin says otherwise. `[player]` arguments that act on someone else need
the command's `.others` permission (admins by default).

**Homes, spawn and warps**

| Command | Description | Default |
|---|---|---|
| `/sethome [name]`, `/home [name]`, `/delhome <name>`, `/renamehome <name> <new>` | Homes. No bed needed, work across dimensions. Admins can use `player:name`. | everyone |
| `/spawn [player]` | Teleport to the spawn | everyone |
| `/setspawn` | Set the spawn where you stand | admins |
| `/back [player]` | Return to where you were before the last teleport or death | everyone |
| `/warp [warp\|page] [player]`, `/warpinfo <warp>` | List warps or teleport to one | everyone |
| `/setwarp <warp>`, `/delwarp <warp>` | Manage warps | admins |

**Teleport requests**

| Command | Description | Default |
|---|---|---|
| `/tpa <player>`, `/tpahere <player>` | Ask to teleport to a player, or ask them to come to you | everyone |
| `/tpaccept [player]`, `/tpdeny [player]`, `/tpacancel [player]` | Answer or cancel requests (chat buttons included) | everyone |
| `/tptoggle [player] [on\|off]`, `/tpauto [player] [on\|off]` | Block requests, or accept them automatically | everyone |
| `/tpaall [player]` | Ask everyone to come to you | admins |

**More teleports**

| Command | Description | Default |
|---|---|---|
| `/tpr` | Random safe spot in the dimension's configured ring | everyone |
| `/tphere <player>`, `/tpo <player> [target]`, `/tpohere <player>` | Bring or send players; `tpo`/`tpohere` ignore `/tptoggle` | admins |
| `/tppos <x> <y> <z> [yaw] [pitch] [world]` | Teleport to coordinates (`~` works) | admins |
| `/tpall [player]`, `/tpoffline <player>` | Bring everyone; go to a player's logout spot | admins |
| `/top`, `/bottom`, `/jump` | Highest or lowest safe spot here, or the block you look at | admins |
| `/world [dimension]` | Switch dimension (Nether at scaled coordinates) | admins |
| `/settpr <world> [center\|minrange\|maxrange] [value]` | Configure `/tpr` | admins |

**Player state**

| Command | Description | Default |
|---|---|---|
| `/fly`, `/god` `[player] [on\|off]` | Flight and invulnerability; kept across relog, respawn and gamemode changes | admins |
| `/heal`, `/feed`, `/rest`, `/ext` `[player]` | Heal (also clears harmful effects), fill hunger, reset phantoms, put out fire | admins |
| `/speed [walk\|fly] <0-10> [player]` | Walking or flying speed (1 = vanilla) | admins |
| `/afk [message]` | Toggle AFK; players also go AFK automatically after 5 minutes idle | everyone |
| `/suicide` | Kill yourself | everyone |
| `/gmc`, `/gms`, `/gma`, `/gmsp` `[targets]`, `/gm` | Gamemode shortcuts; `/gamemode` and `/gm` also accept `c s a sp` and `0-3`. They run vanilla `/gamemode`, so its permission applies | admins |

**Inventory**

| Command | Description | Default |
|---|---|---|
| `/workbench`, `/anvil`, `/cartographytable`, `/grindstone`, `/loom`, `/smithingtable`, `/stonecutter` | Use a block's menu anywhere | admins |
| `/enderchest [player]`, `/invsee <player>` | Open an ender chest; view or edit an online player's inventory | admins |
| `/hat [remove]`, `/repair [hand\|all]`, `/more [amount]` | Wear the held item, repair, fill a stack | admins |
| `/clearinventory [player\|*] [item\|*\|**] [amount]`, `/clearinventoryconfirmtoggle` | Clear items, optionally asking for confirmation | admins |
| `/disposal` | Destroy unwanted items | everyone |

**Kits**

| Command | Description | Default |
|---|---|---|
| `/kit [kit] [player]`, `/showkit <kit>` | List, claim or inspect kits | everyone |
| `/createkit <kit> <delay> [opLevel]`, `/delkit <kit>`, `/kitreset <kit> [player]` | Manage kits. The kit is your current inventory; delay is seconds, `1d2h`, or -1 for one claim | admins |

Items and blocks always use standard IDs such as `minecraft:diamond`.

**Moderation**

| Command | Description | Default |
|---|---|---|
| `/mute <player> [duration] [reason]` | Block chat, `/msg` and `/mail`; run again to unmute | admins |
| `/tempban <player> <duration> [reason]`, `/tempbanip <player\|ip> <duration> [reason]` | Temporary bans on the vanilla ban lists | admins |
| `/unban <player>`, `/unbanip <ip>`, `/kickall [reason]` | Remove bans; kick everyone else | admins |
| `/gc` | Uptime, TPS, memory, chunks and entities | admins |
| `/sudo <player> <command>` | Run a command as a player | OP level 4 |

**Information**

| Command | Description | Default |
|---|---|---|
| `/getpos [player]`, `/compass`, `/depth` | Position, facing, height above sea level | everyone |
| `/seen <player>`, `/playtime [player]`, `/ping` | Last seen, play time, latency | everyone |
| `/near [player] [radius]`, `/whois <player>` | Nearby players; full player details (IP with `whois.ip`) | admins |
| `/motd`, `/rules`, `/info` `[chapter] [page]` | Server texts, editable per language | everyone |
| `/cmdessentials [reload]` (alias `/ess`) | Version info; reload config, messages and texts | everyone / admins |

**Messaging**

| Command | Description | Default |
|---|---|---|
| `/msg <player> <message>`, `/r <message>` | Private messages and replies (`/emsg` is always available) | everyone |
| `/msgtoggle`, `/rtoggle`, `/ignore <player>` | Message acceptance, reply direction and ignored players | everyone |
| `/mail` `read\|clear\|send\|sendtemp\|sendall` | Persistent and expiring player mail | everyone / admins |
| `/helpop <message>`, `/socialspy` | Contact staff; monitor private messages and mail | everyone / admins |
| `/broadcast`, `/broadcastworld` | Server-wide or dimension-wide announcements | admins |
| `/nick [player] <nickname\|off>`, `/realname <nickname>` | Persistent display and tab-list nicknames | admins / everyone |

A player's own teleports wait 3 seconds and are cancelled if they move. Admins skip the wait.

## Configuration

Everything lives in `config/cmdessentials/`, created on first start. Run
`/cmdessentials reload` after editing.

- `config.json`: teleport delay and cooldown, home limit, spawn behaviour, `/back` on death,
  `/tpa` timeout, speed limits, AFK timers (`afk.autoSeconds`, `afk.kickSeconds`), `/near`
  radius, unsafe teleport blocks (by ID), MOTD on join, `/tpr` areas per dimension, and the
  `overrideVanilla` command names.
  Options follow EssentialsX `config.yml` with the same defaults, for example the `/heal`
  cooldown, nickname rules, `spawn-if-no-home`, mail and mute limits, and `disabledCommands`
  (restart needed). The two deliberate differences are a 3-second teleport delay and
  per-player languages being on.
- `messages_<lang>.json`: every message (see [Languages](#languages)).
- `motd_<lang>.txt`, `rules_<lang>.txt`, `info_<lang>.txt`: texts for `/motd`, `/rules` and
  `/info`. Lines starting with `#name` begin a chapter, `##` lines are comments, `&` colour
  codes work, and `{player}`, `{online}`, `{max}` are replaced.

## Languages

English and Simplified Chinese are included. **Each player sees messages and texts in their
own Minecraft language** when a file for it exists. Otherwise the server's `language` setting
is used, then English. To add a language, copy `messages_en_us.json` (and the text files) to,
for example, `messages_ja_jp.json`, translate it and run `/cmdessentials reload`.

## Permissions

Every command is guarded by a permission node of the form `cmdessentials.<command>[.<sub>]`,
for example `cmdessentials.home`, `cmdessentials.spawn.others` or
`cmdessentials.teleport.bypass.delay`. `cmdessentials.sethome.limit` is a numeric option: a
LuckPerms meta value on Fabric, or an integer node on Forge.

- **Fabric:** if a provider of
  [fabric-permissions-api](https://github.com/lucko/fabric-permissions-api) is installed, such
  as [LuckPerms](https://luckperms.net/), it decides. It isn't required.
- **Forge:** the nodes are registered with Forge's `PermissionAPI`, so any Forge permission
  handler can manage them.
- **No permission mod:** each node falls back to a vanilla OP level. That is level 0 for
  everyday player commands and level 2 for admin commands.

## Roadmap

The full plan lives in [AGENTS.md](AGENTS.md#commands). It is based on the 163 EssentialsX
commands listed in [docs/essentials-commands.md](docs/essentials-commands.md).

- **P0 (done):** homes, spawn, `/speed`, `/back`, `/tpa` and friends, warps
- **P1 (done):** player state and AFK, info commands, teleport extensions, gamemode shortcuts,
  messaging, mail, nicknames, inventory tools, portable workbenches, kits
- **P2 (in progress):** moderation is done; next are jails, `/vanish` and the `e`-prefixed
  vanilla replacements, per-player time and weather, world tools, item tools and power tools
- **P3 (optional):** economy, fun commands
- **Out of scope:** Discord/XMPP integration, `/customtext`, `/backup`

Vanilla commands are not replaced by default. Where the names clash, the Essentials version
gets an `e`-prefixed name (`/egive`, `/etp`, …). The exception is private messages:
`/msg`, `/tell` and `/w` are replaced by default so that `/r`, `/ignore` and `/socialspy`
work (configurable with `overrideVanilla`). Shortcuts like `/gmc` only call the vanilla
command.

## Building from source

You need JDK 21 to run Gradle. JDK 17 is also needed for the 1.18.2 and 1.20.1 targets, and
Gradle's toolchain support finds or downloads it.

```bash
./gradlew buildAndCollect
```

All 6 jars end up in `build/libs/all/`. Unit tests run with `./gradlew :common:1.20.1:test`.
GitHub Actions builds the jars and runs the tests on every push to `main` and every pull
request. Download the jars from the run's **Artifacts**.

### Development

The project uses [Architectury Loom](https://github.com/architectury/architectury-loom) for
the `common` / `fabric` / `forge` split. It uses [Stonecutter](https://stonecutter.kikugie.dev/)
to build every Minecraft version from one source tree.

```
common/   all logic: commands, permissions, version adapters (vanilla + Brigadier only)
fabric/   Fabric entrypoint and platform implementation
forge/    Forge entrypoint and platform implementation
versions/ per-version properties (dependency versions, Java version, …)
```

Useful tasks:

```bash
./gradlew "Set active project to 1.21.1"   # switch the version the sources are set up for
./gradlew runActiveServerFabric            # dev server for the active version
./gradlew "Reset active project"           # back to 1.20.1 before committing
```

Only the active version can be built or run on its own. A dev server doesn't exit after `stop`;
press Ctrl+C once the world is saved. [AGENTS.md](AGENTS.md#build--run) covers these and other
details, such as testing Forge 1.21.1 on a real server. In-game test checklists are in
[docs/testing/](docs/testing/).

## License

[MIT](LICENSE)

## Credits

Command design follows [EssentialsX](https://essentialsx.net/). This project is not affiliated
with the EssentialsX team.
