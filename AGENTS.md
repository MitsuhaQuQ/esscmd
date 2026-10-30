# cmdEssentials

A **server-only** Minecraft mod that brings the commands and features of the Bukkit
[EssentialsX](https://essentialsx.net/commands) plugin to Fabric and Forge servers.
Vanilla clients must be able to join a server running this mod without installing anything.

Read this file before changing code. Reply to the user in Chinese; write code, comments and
commit messages in English.

## Status

The build is scaffolded: all 6 jars build, and only `/cmdessentials` (alias `/ess`) is
implemented. The command checklist under **Commands** tracks
progress.

## Target versions

| Minecraft | Java | Loaders       |
|-----------|------|---------------|
| 1.18.2    | 17   | Fabric, Forge |
| 1.20.1    | 17   | Fabric, Forge |
| 1.21.1    | 21   | Fabric, Forge |

That makes 3 versions × 2 loaders = 6 jars. Don't add another version until all of these build
and pass their tests. NeoForge is out of scope for now. If we add it later, it becomes a fourth
module (`neoforge`) next to `forge`, and `common` stays unchanged.

## Architecture

Two axes: **loaders** are separate Gradle modules, and **Minecraft versions** are handled by
Stonecutter inside one source tree.

```
cmdEssentials/
├─ settings.gradle.kts         # Stonecutter tree: versions × branches {"" (root), common, fabric, forge}
├─ stonecutter.gradle.kts      # plugin versions, active version, buildAndCollect / runActiveServer* tasks
├─ build.gradle.kts            # script for the root nodes (`:1.20.1`): empty, they only hold properties
├─ gradle.properties           # mod.* metadata, shared deps.* (fabric loader, permissions API)
├─ versions/<mc>/gradle.properties  # per-version: java, mc ranges, pack_format, fabric api, forge
├─ buildSrc/                   # build helpers: `mod`, `versionProp(...)`, `properties(...)`
├─ common/                     # ALL logic. Only vanilla Minecraft (Mojmap) + Brigadier
│  └─ src/main/java/com/mtmrails/esscmd/
│     ├─ CmdEssentials.java    # init() and registerCommands(): the hooks loaders call
│     ├─ platform/Platform.java  # the loader abstraction (ServiceLoader)
│     ├─ permission/           # PermissionNode, PermissionNodes (the one node list)
│     ├─ compat/               # version adapters, the only place with //? blocks
│     └─ command/              # one class per command
├─ fabric/                     # entrypoint, FabricPlatform, fabric.mod.json
└─ forge/                      # @Mod class, ForgePlatform, mods.toml, pack.mcmeta
```

### Gradle

- Every module uses **Architectury Loom**. `common` is declared with
  `architectury { common("fabric", "forge") }`, and `fabric` / `forge` with
  `architectury { fabric() }` / `{ forge() }`. The loader modules bundle `common` into their jar
  through the `shadow` / `transformProductionX` setup.
- Use **only the Loom plugin**. Don't add a runtime dependency on Architectury API: the jars
  must depend on nothing except the loader and Fabric API.
- `fabric-permissions-api` is `modCompileOnly` and a *soft* dependency: `FabricPlatform` checks
  `isModLoaded("fabric-permissions-api-v0")` (LuckPerms ships it) and falls back to OP levels.
  Don't bundle it.
- Pinned tool versions: Gradle 8.12 (wrapper), Stonecutter 0.6.2, Architectury Loom 1.9.436,
  architectury-plugin 3.4.164, shadow 8.1.1. Upgrade them together and rebuild all 6 jars.
  architectury-plugin 3.4.138 and older fail on Gradle 8.12.
- **Don't set `group`** in the module scripts. Every node project is named after its version
  (`:common:1.20.1`, `:fabric:1.20.1`), so a shared group gives them identical coordinates.
  Gradle then fails with "no variant with that configuration name exists".
- Per-version values: read them with `versionProp("key")`, which looks in
  `versions/<mc>/gradle.properties` through the root node. Loader nodes can't see that file
  directly.
- Mappings: `loom.officialMojangMappings()` everywhere.
- Every version string lives in `gradle.properties` or `versions/<mc>/gradle.properties`.
  Never hard-code a version in a build script.
- Java toolchain comes from the per-version `java` property: 17 or 21. Gradle itself runs on
  JDK 21.
- `processResources` expands `fabric.mod.json`, `META-INF/mods.toml` and `pack.mcmeta` from
  properties (`${id}`, `${version}`, `${minecraft}`, `${forge_major}`, `${pack_format}`, …).
  These templates must not contain any per-version branching.
- Jar name: `cmdessentials-<loader>-<mod_version>+<mc>.jar`. `buildAndCollect` copies all 6
  jars to `build/libs/all/`.

### Code split

- **`common`** may import `net.minecraft.*`, `com.mojang.brigadier.*` and our own code only.
  It must never import `net.fabricmc.*` or `net.minecraftforge.*`.
- Loader-specific features go through **`platform.Platform`** in `common` (loader name, mod
  version, config dir, permission checks). There is one implementation per loader
  (`FabricPlatform`, `ForgePlatform`), loaded with `java.util.ServiceLoader` from
  `META-INF/services/com.mtmrails.esscmd.platform.Platform`. Add new loader needs as
  methods there. Don't use `@ExpectPlatform`, because it needs Architectury's transformer at
  runtime.
- **Events**: `common` exposes plain hooks. These exist now: `CmdEssentials.init()` and
  `registerCommands(dispatcher)`. Add these as features need them: `onServerStarted/Stopping`,
  `onPlayerJoin/Leave/Death/Respawn`, `onPlayerChangeDimension`, `onServerTick`. When a command
  needs `CommandBuildContext` (1.19+ only), add it behind a `//? if >=1.19` condition.
  Each loader module subscribes to its own events (Fabric callbacks, the Forge event bus) and
  only forwards them. The loader modules contain no logic beyond that.
  - Command registration: Fabric uses `CommandRegistrationCallback` (v2 API on 1.19+, v1 on
    1.18.2); Forge uses `RegisterCommandsEvent`.
  - Permissions: Fabric uses `Permissions.check(source, node, opLevel)`. Forge registers every
    node in `PermissionGatherEvent.Nodes` and then checks it with `PermissionAPI`. Keep the
    node list in `common` (one `PermissionNodes` class) so both loaders use the same list.
- **Mixins**: there are none yet. The first one goes in `common`, in
  `cmdessentials.common.mixins.json`. Register that config in `fabric.mod.json` and through
  `loom.forge.mixinConfigs(...)`. Only add loader-specific mixins when unavoidable. Prefer
  events over mixins.
- **Server-only flag**: Fabric uses `"environment": "server"` with a `server` entrypoint
  (`DedicatedServerModInitializer`). Forge sets `displayTest = "IGNORE_SERVER_VERSION"` in
  mods.toml. On 1.18.2, `CmdEssentialsForge` also registers `IExtensionPoint.DisplayTest` in
  code, behind `//? if <1.20`.

### Multi-version with Stonecutter

- Minecraft API differences are handled with Stonecutter comments on the sources:
  `//? if >=1.20.5 {` … `//?} else {` … `//?}`. Use version-range conditions only, never
  loader conditions. Loader differences belong in the loader modules.
- Put version branches in **one place**: a small adapter class per topic in
  `common/.../compat/` (`Texts`, `ItemData`, `Teleports`, `Attributes`, …). Command code calls
  these adapters and contains no `//?` blocks itself.
- Known version splits we need to handle: 1.19 (chat and `Component` API, the
  `CommandBuildContext` argument), 1.20 (`Level` vs `ServerLevel` getters, smithing menu),
  1.20.5 (NBT → data components, attribute modifier IDs), 1.21 (`ResourceLocation` factory
  methods).
- The source files on disk are always in the state of the **active** version. Before you
  commit, switch back to the VCS version (1.20.1) so that diffs stay readable.

### Commands

Register commands with Brigadier only. Put each command in its own class under
`common/.../command/`. Each class exposes a static `register(dispatcher)` method that is called
from `CmdEssentials.registerCommands`. Guard every node with a `PermissionNodes` entry through
`.requires(NODE.require())`. For an alias with no arguments, copy `executes` onto the alias as
well as setting `redirect`, because a bare Brigadier redirect does nothing.
`EssentialsCommand` shows the pattern.

## Server-only rules (hard constraints)

- Never reference client classes (`net.minecraft.client.*`, `@Environment(EnvType.CLIENT)`,
  `Dist.CLIENT`).
- Mark the mod as server-only on both loaders, so vanilla clients aren't rejected (see
  **Code split → Server-only flag**). Don't add any client entrypoint.
- Don't add custom items, blocks, entities, network packets, registries or resource-pack
  content. Anything a vanilla client can't render is not allowed.
- Don't use translation keys from this mod (`Component.translatable("cmdessentials.*")`): the
  client has no lang file for them. Build messages on the server from our own message files
  (see **Messages**). Vanilla translation keys are fine.

## Permissions

- Fabric: `fabric-permissions-api` (works with LuckPerms). Forge: `PermissionAPI`. How each
  loader wires this up is described under **Code split**.
- With no permission provider, fall back to an OP level per node (default: basic player
  commands = 0, admin commands = 2).
- Node format: `cmdessentials.<command>[.<sub>]`, e.g. `cmdessentials.home`,
  `cmdessentials.home.others`, `cmdessentials.sethome.multiple`, `cmdessentials.speed.others`.
- Any command that acts on another player needs its own `.others` node.

## Data & config

- Per-world data (homes, warps, the spawn point, `/back` locations) is stored in `SavedData`
  under `<world>/data/cmdessentials_*.dat`. Store dimension, position, yaw and pitch.
- Config: `config/cmdessentials.toml`, reloadable with `/cmdessentials reload` (no restart).
  Settings to cover: home limit, teleport delay, cooldowns, `/tpa` timeout, speed caps, and
  cancelling a teleport when the player moves.
- Never block the server thread on file I/O while the server is running.

## Messages

- Every user-facing string comes from `config/cmdessentials/messages_<lang>.json` (default
  `en_us`; ship `zh_cn` too), and is turned into a `Component` on the server.
- Use one consistent style: gold prefix, gray body, and red for errors.

## Commands

The command set follows EssentialsX. Names, aliases and syntax for all 163 commands are in
[docs/essentials-commands.md](docs/essentials-commands.md), a text copy of `EssentialsX.pdf`.
Use that file and don't parse the PDF. Match Essentials' syntax and behaviour unless a rule
below says otherwise. Mark each line `[x]` once every command on it is done.

### Rules

- **Aliases**: register the aliases from the reference as Brigadier redirects. Skip the
  `e`-prefixed ones (`eafk`, `etpa`, …), except for commands that clash with vanilla.
- **Vanilla clashes**: never replace a vanilla command by default. These clash:
  `/ban`, `/kick`, `/kill`, `/list`, `/me`, `/msg`, `/give`, `/gamemode`, `/help`, `/time`,
  `/weather`, `/tp`, `/enchant`. So do these aliases: `/tell`, `/w`, `/clear`, `/pardon`, `/xp`,
  `/teleport`. Register the Essentials version only under its `e` name (`/emsg`, `/egive`, …)
  and drop the clashing aliases. Admins can opt in through a config list `override-vanilla`;
  to override, remove the vanilla node from the dispatcher root before registering ours.
- **Players**: parse `[player]` with `EntityArgument`. Acting on another player needs the
  command's `.others` permission node.
- **`[player:]name`** (homes): lets admins manage other players' homes, under the
  `cmdessentials.home.others` node.
- **`datediff`** (`1d2h30m`): one shared parser, used by `/mute`, `/tempban`, `/togglejail`,
  `/mail sendtemp`, …
- **Items**: use vanilla resource IDs (`diamond`, `minecraft:diamond`). There are no numeric
  IDs, no `:data` suffix and no Essentials `items.csv`. Item NBT changed to data components in
  1.20.5, so hide that difference behind a version helper.
- **"world"** in Essentials means a dimension here (`/world`, `/broadcastworld`, `/time … <world>`).
- **Container commands** (`/workbench`, `/anvil`, `/grindstone`, …): open the vanilla menu with
  `ContainerLevelAccess.NULL` (or override `stillValid`) so it doesn't close for lack of a real
  block. Check that the menu exists in each target version, because smithing changed in 1.20.
- All teleports go through one shared teleport service. It handles the delay, cancelling the
  teleport if the player moves, cooldowns, `/tptoggle`, saving the `/back` location, and
  moving between dimensions.

### P0: homes, spawn, teleport basics

- [ ] `/sethome`, `/home`, `/delhome`, `/renamehome`: homes don't need a bed and work across
  dimensions. `/home` with no name lists your homes if you have more than one (Essentials has
  no `/homes`). The number of homes is limited by the config or the
  `cmdessentials.sethome.multiple.<n>` node.
- [ ] `/setspawn`, `/spawn`: `/setspawn` sets the spawn and `/spawn` teleports to it, falling
  back to the world spawn if none is set. Options: respawn there after death and on first join.
- [ ] `/speed [walk|fly] <0-10> [player]`: with no type given, use `fly` while the player is
  flying and `walk` otherwise. `1` = vanilla default.
  - Fly: set `Abilities.flyingSpeed`, then call `onUpdateAbilities()`.
  - Walk: add or replace a transient `MOVEMENT_SPEED` attribute modifier. Don't use
    `Abilities.walkingSpeed`, which only changes the client's FOV.
  - Mapping: below 1, scale the default linearly (`default * v`); from 1 to 10, interpolate
    linearly from the default to a configured maximum.
- [ ] `/back`: your location before the last teleport. Optionally your death location too.
- [ ] `/tpa`, `/tpahere`, `/tpaccept`, `/tpdeny`, `/tpacancel`, `/tptoggle`, `/tpauto`
- [ ] `/warp`, `/setwarp`, `/delwarp`, `/warpinfo`
- [ ] `/essentials` → `/cmdessentials` (alias `/ess`): `reload` and version info

### P1: player utilities, chat, inventory

- [ ] Teleport: `/tphere`, `/tppos`, `/tpo`, `/tpohere`, `/tpall`, `/tpaall`, `/tpoffline`,
  `/top`, `/bottom`, `/jump`, `/tpr`, `/settpr`, `/world`
- [ ] Player state: `/fly`, `/god`, `/heal`, `/feed`, `/rest`, `/ext`, `/afk`, `/suicide`
- [ ] Info: `/getpos`, `/compass`, `/depth`, `/near`, `/seen`, `/whois`, `/playtime`, `/ping`,
  `/realname`, `/motd`, `/rules`, `/info`
- [ ] Messaging: `/msg` (as `/emsg`), `/r`, `/msgtoggle`, `/rtoggle`, `/ignore`, `/socialspy`,
  `/helpop`, `/mail`, `/broadcast`, `/broadcastworld`, `/nick`
- [ ] Inventory: `/enderchest`, `/invsee`, `/hat`, `/repair`, `/more`, `/clearinventory`,
  `/clearinventoryconfirmtoggle`, `/disposal`
- [ ] Portable blocks: `/workbench`, `/anvil`, `/cartographytable`, `/grindstone`, `/loom`,
  `/smithingtable`, `/stonecutter`
- [ ] Kits: `/kit`, `/createkit`, `/delkit`, `/showkit`, `/kitreset`

### P2: admin, world, items

- [ ] Moderation: `/mute`, `/tempban`, `/tempbanip`, `/unban`, `/unbanip`, `/kickall`,
  `/vanish`, `/sudo`, `/gc`
- [ ] Vanilla-clash versions (only as `e` names): `/ban`, `/banip`, `/kick`, `/kill`, `/list`,
  `/me`, `/gamemode`, `/time`, `/weather`, `/give`, `/tp`
- [ ] Jail: `/setjail`, `/deljail`, `/jails`, `/togglejail`, `/jailedplayers`
- [ ] Per-player view: `/ptime`, `/pweather`. These change only that player's client, through
  packets, so send them again after a dimension change or respawn.
- [ ] World: `/thunder`, `/lightning`, `/burn`, `/ice`, `/remove`, `/spawner`, `/spawnmob`,
  `/tree`, `/bigtree`, `/break`
- [ ] Items: `/item`, `/itemdb`, `/itemname`, `/itemlore`, `/enchant`, `/exp`, `/skull`,
  `/book`, `/firework`, `/potion`, `/condense`, `/recipe`, `/editsign`, `/unlimited`
- [ ] Power tools: `/powertool`, `/powertoollist`, `/powertooltoggle`

### P3: optional modules (each behind a config switch)

- [ ] Economy: `/balance`, `/balancetop`, `/pay`, `/paytoggle`, `/payconfirmtoggle`, `/eco`,
  `/sell`, `/worth`, `/setworth`. Decide first whether we store balances ourselves or use an
  existing economy mod's API.
- [ ] Fun: `/antioch`, `/nuke`, `/beezooka`, `/kittycannon`, `/fireball`
- [ ] Chat: `/toggleshout`. This needs a local and shout chat range system first.

### Out of scope

- `/discord`, `/discordbroadcast`, `/link`, `/unlink`, `/setxmpp`, `/xmpp`, `/xmppspy`: these
  are integrations with external services.
- `/customtext`: configured in `bukkit.yml`, so it doesn't apply here.
- `/backup`: leave this to a backup mod.
- `/help`: vanilla `/help` already lists Brigadier commands.

## Build & run

Run Gradle with **JDK 21**: `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`. Gradle 8.12
can't run on the JDK 25 that is this machine's default `java`. The toolchain picks JDK 17 or
21 for each version on its own (both are installed under `/Library/Java`).

| Goal | Command |
|---|---|
| Build all 6 jars into `build/libs/all/` | `./gradlew buildAndCollect` |
| Switch the active version | `./gradlew "Set active project to 1.21.1"` |
| Build the active version only | `./gradlew :fabric:1.21.1:build` (node path: `:<branch>:<mc>`) |
| Dev server, active version | `./gradlew runActiveServerFabric` / `runActiveServerForge` |
| Back to the VCS version before committing | `./gradlew "Reset active project"` |

**Only the active version can be built or run on its own.** Stonecutter 0.6 generates the
sources for the other versions only inside `buildAndCollect`. A plain
`:fabric:1.18.2:runServer` while 1.20.1 is active compiles an empty source set: the server
starts without our classes on Fabric, and Forge fails with "contained no existing paths".
To test another version, switch to it, test, then reset.

- Dev servers run in `run/<mc>-<loader>/`, and their EULA is accepted automatically. Only
  server run configs exist; the client run config is removed on purpose.
- **`:forge:1.21.1:runServer` doesn't work.** Architectury Loom 1.9 and 1.10 assemble a broken
  module path for Forge 1.21.1 dev runs ("Module org.lwjgl.openal not found"). Excluding LWJGL
  only moves the failure further into Forge's bootstrap. Test Forge 1.21.1 on a real server
  instead: install it with the official installer
  (`forge-1.21.1-<ver>-installer.jar --installServer`), put
  `build/libs/all/cmdessentials-forge-*+1.21.1.jar` in `mods/`, and run it. The other 5
  loader/version pairs work with `runServer`.
- Test with a **vanilla client** of the same Minecraft version connected to the dev server.
  This also proves the server-only setup works. Never test in singleplayer only.
- From a script, you can pipe console commands into `runServer` through stdin (for example
  `cmdessentials`, then `stop`).
- **Dev servers don't exit after `stop`.** The Architectury Transformer dev launcher leaves a
  non-daemon tiny-remapper thread pool running. Wait for `All dimensions are saved`, then kill
  the process (Ctrl+C, or `pkill -f "<branch>/versions/<mc>/.gradle/architectury"`). This
  happens only in dev; production jars don't contain that launcher.
- Before finishing a task, run `./gradlew buildAndCollect`. A change that compiles on only the
  active version isn't done.
- In zsh, write node paths with braces (`":fabric:${mc}:build"`). zsh reads `$mc:r` or
  `$mc:s` as a variable modifier and mangles the path.
- Planned: GameTest-based tests in `common` for the services (teleport, homes, datediff parser,
  speed mapping), run on every target with `runGameTestServer`.
