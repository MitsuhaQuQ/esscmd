# esscmd (cmdEssentials)

A **server-only** Minecraft mod that brings the commands of the Bukkit
[EssentialsX](https://essentialsx.net/commands) plugin to Fabric and Forge servers.

Install it on the server only. Players join with an unmodified vanilla client.

> **Status: early development.** The multi-loader, multi-version build works, but only the
> `/cmdessentials` command exists so far. Homes, spawn, `/speed`, `/tpa`, warps and the rest are
> planned. See the [roadmap](#roadmap).

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

| Command | Description | Permission | Default |
|---|---|---|---|
| `/cmdessentials` (alias `/ess`) | Shows the mod version and loader | `cmdessentials.essentials` | everyone |
| `/cmdessentials reload` | Reloads the configuration | `cmdessentials.essentials.reload` | OP level 2 |

There is no configuration file yet, so `reload` currently has nothing to reload.

## Permissions

Every command is guarded by a permission node of the form `cmdessentials.<command>[.<sub>]`.

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

- **P0:** `/sethome` `/home` `/delhome` `/renamehome`, `/setspawn` `/spawn`, `/speed`, `/back`,
  `/tpa` and friends, warps
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
