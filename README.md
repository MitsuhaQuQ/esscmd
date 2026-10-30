# esscmd (cmdEssentials)

A **server-only** Minecraft mod that brings the commands of the Bukkit
[EssentialsX](https://essentialsx.net/commands) plugin to Fabric and Forge servers.

Install it on the server only. Players join with an unmodified vanilla client.

> **Status: early development.** The first batch of commands (homes, spawn, `/speed`, `/back`,
> `/tpa`, warps) is implemented. More are planned; see the [roadmap](#roadmap).

## Supported versions

| Minecraft | Fabric | Forge | Java |
|-----------|:------:|:-----:|:----:|
| 1.18.2    | ✅     | ✅    | 17   |
| 1.20.1    | ✅     | ✅    | 17   |
| 1.21.1    | ✅     | ✅    | 21   |

NeoForge is not supported yet.

## Installation

1. Download the jar that matches your loader and Minecraft version, for example
   `cmdessentials-fabric-0.1.0+1.20.1.jar` or `cmdessentials-forge-0.1.0+1.21.1.jar`.
2. Put it in the server's `mods/` folder.
3. **Fabric only:** also install [Fabric API](https://modrinth.com/mod/fabric-api).
4. Start the server.

The mod doesn't load in singleplayer or on clients. That is intentional.

## Commands

Syntax follows [EssentialsX](https://essentialsx.net/commands). "Admins" means OP level 2
unless a permission plugin says otherwise.

| Command | Description | Default |
|---|---|---|
| `/sethome [name]`, `/home [name]`, `/delhome <name>`, `/renamehome <name> <new>` | Homes. No bed needed, work across dimensions. Admins can use `player:name`. | everyone |
| `/spawn [player]` | Teleport to the spawn (others: admins) | everyone |
| `/setspawn` | Set the spawn where you stand | admins |
| `/back [player]` | Return to where you were before the last teleport or death | everyone |
| `/tpa <player>`, `/tpahere <player>` | Ask to teleport to a player, or ask them to come to you | everyone |
| `/tpaccept [player]`, `/tpdeny [player]`, `/tpacancel [player]` | Answer or cancel requests (chat buttons included) | everyone |
| `/tptoggle [player] [on\|off]`, `/tpauto [player] [on\|off]` | Block requests, or accept them automatically | everyone |
| `/warp [warp] [player]`, `/warpinfo <warp>` | List warps or teleport to one | everyone |
| `/setwarp <warp>`, `/delwarp <warp>` | Manage warps | admins |
| `/speed [walk\|fly] <0-10> [player]` | Walking or flying speed (1 = vanilla) | admins |
| `/cmdessentials [reload]` (alias `/ess`) | Version info; reload config and messages | everyone / admins |

A player's own teleports wait 3 seconds and are cancelled if they move. Admins skip the wait.

## Configuration

`config/cmdessentials/config.json` is created on first start. It covers the teleport delay
and cooldown, home limit, spawn behaviour, `/back` on death, `/tpa` timeout and the speed
limits. Run `/cmdessentials reload` after editing it.

## Languages

Messages live in `config/cmdessentials/messages_<lang>.json`. English and Simplified Chinese
are included. **Each player sees messages in their own Minecraft language** when a file for
it exists. Otherwise the server's `language` setting is used, then English. To add a
language, copy `messages_en_us.json` to, for example, `messages_ja_jp.json`, translate it and
run `/cmdessentials reload`.

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

- **P0 (done):** `/sethome` `/home` `/delhome` `/renamehome`, `/setspawn` `/spawn`, `/speed`,
  `/back`, `/tpa` and friends, warps
- **P1:** more teleport commands, player state (`/fly`, `/god`, `/heal`, …), messaging and
  mail, inventory tools, portable workbenches, kits
- **P2:** moderation, jails, world and item tools
- **P3 (optional):** economy, fun commands
- **Out of scope:** Discord/XMPP integration, `/customtext`, `/backup`

Vanilla commands (`/msg`, `/give`, `/tp`, …) are never replaced by default. The Essentials
versions are registered under their `e`-prefixed names (`/emsg`, `/egive`, …).

## Building from source

You need JDK 21 to run Gradle. JDK 17 is also needed for the 1.18.2 and 1.20.1 targets, and
Gradle's toolchain support finds or downloads it.

```bash
./gradlew buildAndCollect
```

All 6 jars end up in `build/libs/all/`.

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
details, such as testing Forge 1.21.1 on a real server.

## Credits

Command design follows [EssentialsX](https://essentialsx.net/). This project is not affiliated
with the EssentialsX team.
