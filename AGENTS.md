# cmdEssentials

A **server-side** Minecraft mod that brings the commands and features of the Bukkit
[EssentialsX](https://essentialsx.net/commands) plugin to Fabric and Forge servers.
Vanilla clients must be able to join a server running this mod without installing anything.
Installed on a client, it also works in singleplayer and LAN worlds.

Read this file before changing code. [docs/architecture.md](docs/architecture.md) describes the
modules and runtime flow with UML diagrams; update it when the structure changes. Reply to the user in Chinese; write code, comments and
commit messages in English.

Keep the conversation language constant: every message to the user stays in Chinese for the
whole session, including progress notes, summaries, questions and the final report. Don't switch
to English because a tool result, log, file or earlier English message is in English, and don't
mix languages within a message. Identifiers, commands, paths and quoted output stay as they are.

## Status

All 8 jars build; CI builds them and runs the unit tests on every push.

- **Done:** P0, P1 (M1–M7) and P2 (M8–M12).
- **In-game testing:** P0 and M2–M7 on Fabric 1.20.1. P0 and M5 core checks on Forge 1.20.1 and
  Fabric 1.21.1. Both 1.18.2 targets passed a pass covering P0, M5, M6, M7 and M8 core checks.
- **Client install:** singleplayer passed (user-tested).
- **Not yet tested in-game:** `/book`, `/firework`, `/potion`, chat commands hidden in
  singleplayer, Forge 1.21.1 (console only), `/mute` with signed chat (1.19+),
  all of M9–M12 and all of 26.3. Both 26.3 targets pass a console smoke test with mixin
  verification (Forge on a real server, `testserver/26.3-forge`).

The checklists under **Commands** track progress; test steps are in `docs/testing/`.

## Target versions

| Minecraft | Java | Loaders       |
|-----------|------|---------------|
| 1.18.2    | 17   | Fabric, Forge |
| 1.20.1    | 17   | Fabric, Forge |
| 1.21.1    | 21   | Fabric, Forge |
| 26.3      | 25   | Fabric, Forge |

That makes 4 versions × 2 loaders = 8 jars. Don't add another version until all of these build
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
│     ├─ CmdEssentials.java    # the hooks loaders call (init, commands, tick, join, death, ...)
│     ├─ platform/Platform.java  # the loader abstraction (ServiceLoader)
│     ├─ permission/           # PermissionNode, IntOption, PermissionNodes (the one list)
│     ├─ config/               # EssConfig (JSON) and ConfigManager (load, async reload)
│     ├─ message/              # Messages (per-player), TextFiles/TextDocument, Broadcast,
│     │                        #   MessagingService (/r history), Nicknames
│     ├─ data/                 # EssData (SavedData), PlayerRecord, StoredLocation, Kit,
│     │                        #   MailMessage
│     ├─ teleport/             # TeleportService (delay, cooldown, /back), TpaRequests,
│     │                        #   SafeSpots, RandomTeleport
│     ├─ player/               # AbilityService, AfkService, Sessions, Mutes, JailService,
│     │                        #   VanishService, PlayerView, ItemPowers
│     ├─ menu/                 # PortableMenus, InvseeMenu, InvseeContainer
│     ├─ util/                 # DateDiff, Pager (pure Java, unit-tested)
│     ├─ compat/               # version adapters, the only place with //? blocks
│     │  └─ mixin/             # mixins (cmdessentials.common.mixins.json)
│     └─ command/              # one class per command or command family
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
- Pinned tool versions: Gradle 9.5.0 (wrapper), Stonecutter 0.6.2, Architectury Loom 1.17.493,
  architectury-plugin 3.5.170, shadow 9.6.1 (`com.gradleup.shadow`). Upgrade them together and
  rebuild all 8 jars. architectury-plugin 3.4.x fails on Loom 1.17.
- **26.1+ is unobfuscated**, so its nodes use `build.unobf.gradle.kts` (set in
  `settings.gradle.kts`) in `common`, `fabric` and `forge`: `dev.architectury.loom-no-remap`, no
  mappings, no remap and no Architectury plugin. `common` is compiled as it is and its classes
  are copied into the loader jar. When adding a 26.x version, add it to that list too.
- **Don't set `group`** in the module scripts. Every node project is named after its version
  (`:common:1.20.1`, `:fabric:1.20.1`), so a shared group gives them identical coordinates.
  Gradle then fails with "no variant with that configuration name exists".
- Per-version values: read them with `versionProp("key")`, which looks in
  `versions/<mc>/gradle.properties` through the root node. Loader nodes can't see that file
  directly.
- Mappings: `loom.officialMojangMappings()` everywhere.
- Every version string lives in `gradle.properties` or `versions/<mc>/gradle.properties`.
  Never hard-code a version in a build script.
- Java toolchain comes from the per-version `java` property: 17, 21 or 25. Gradle itself runs
  on JDK 21.
- `processResources` expands `fabric.mod.json`, `META-INF/mods.toml` and `pack.mcmeta` from
  properties (`${id}`, `${version}`, `${minecraft}`, `${forge_major}`, `${pack_format}`, …).
  These templates must not contain any per-version branching.
- Jar name: `cmdessentials-<loader>-<mod_version>+<mc>.jar`. `buildAndCollect` copies all 8
  jars to `build/libs/all/`.

### Code split

- **`common`** may import `net.minecraft.*`, `com.mojang.brigadier.*` and our own code only.
  It must never import `net.fabricmc.*` or `net.minecraftforge.*`.
- Loader-specific features go through **`platform.Platform`** in `common` (loader name, mod
  version, config dir, permission checks, integer options). There is one implementation per loader
  (`FabricPlatform`, `ForgePlatform`), loaded with `java.util.ServiceLoader` from
  `META-INF/services/com.mtmrails.esscmd.platform.Platform`. Add new loader needs as
  methods there. Don't use `@ExpectPlatform`, because it needs Architectury's transformer at
  runtime.
- **Events**: `common` exposes plain hooks on `CmdEssentials`: `init`, `registerCommands`,
  `onServerStopping`, `onServerTick`, `onPlayerJoin/Leave`, `onPlayerDeath` (dead entity, still
  at the death spot) and `onPlayerRespawn` (new entity, only after death, not after the End).
  Add more as features need them. When a command needs `CommandBuildContext` (1.19+ only),
  add it behind a `//? if >=1.19` condition.
  Each loader module subscribes to its own events (Fabric callbacks, the Forge event bus) and
  only forwards them. The loader modules contain no logic beyond that.
  - Command registration: Fabric uses `CommandRegistrationCallback` (v2 API on 1.19+, v1 on
    1.18.2); Forge uses `RegisterCommandsEvent`.
  - Death on Fabric: Fabric API for 1.18.2 has no death event, so `ServerPlayerEvents.AFTER_RESPAWN`
    with `alive == false` calls `onPlayerDeath(oldPlayer)` then `onPlayerRespawn(newPlayer)`.
    Forge uses `LivingDeathEvent` and `PlayerRespawnEvent`.
  - Server tick on Forge: `TickEvent.ServerTickEvent.Post` on 1.21+, the phase-checked event
    before. `getServer()` on that event only exists from 1.20.
  - Permissions: Fabric uses `Permissions.check(source, node, opLevel)`. Forge registers every
    node in `PermissionGatherEvent.Nodes` and then checks it with `PermissionAPI`. Keep the
    node list in `common` (one `PermissionNodes` class) so both loaders use the same list.
- **Mixins** live in `common/.../compat/mixin/` and are listed in
  `cmdessentials.common.mixins.json`, which `fabric.mod.json` and `loom.forge.mixinConfigs(...)`
  register. Architectury Loom rewrites the names in the mixin annotations directly (intermediary
  on Fabric, SRG on Forge before 1.20.5), so the jars have no refmap. Only add
  loader-specific mixins when unavoidable. Prefer events over mixins. Current mixins:
  - `ServerPlayerMixin`: before 1.20.2 vanilla doesn't keep the client's language, so it
    stores it (see `Players.language`). From 1.20.2 it's empty and `clientInformation()` is used.
  - `ServerGamePacketListenerImplMixin`: chat and command activity for AFK, plus `/mute`.
    Expected injected handlers: 2 on 1.18.2, 3 on 1.20.1, 4 on 1.21.1 and 26.3.
  - `TrackedEntityMixin`, `ChunkMapAccessor`: /vanish. `ServerPlayerGameModeMixin`: /powertool.
  - `PlayerMixin`, `ServerPlayerListNameMixin`: nickname in the display name, nickname and
    AFK tag in the tab list. `TabListNames` sends the update packet.
  - **Verifying mixins**: targets like the network handler only load when a player joins, so
    a broken injection would crash then. Start a server with
    `-Dcmdessentials.debug.loadMixinTargets=true` (loads every target at startup; add new
    targets to `CmdEssentials.loadMixinTargets`) and `-Dmixin.debug.export=true`, then check
    the injected `handler$...` methods with `javap -p` in `.mixin.out/`. On dev servers pass
    both through `JAVA_TOOL_OPTIONS`.
  - Never target library classes such as Brigadier: Forge doesn't transform them, so the
    mixin silently doesn't apply and casts crash at startup. `compat/CommandTrees` uses
    reflection to remove vanilla commands instead.
- **Sides (decided: server + client)**: the mod loads on dedicated servers and on clients, where it
  runs on the integrated server (singleplayer, LAN). It is optional on the client: vanilla
  clients join, and a client with it joins servers without it. Fabric uses `"environment": "*"`
  with a `main` entrypoint (`ModInitializer`), which also runs on clients, so it may only register
  server events. Forge sets `displayTest = "IGNORE_SERVER_VERSION"` and `side = "BOTH"` in
  mods.toml. On 1.18.2, `CmdEssentialsForge` also registers `IExtensionPoint.DisplayTest` in
  code, behind `//? if <1.20`. Static in-memory state must be cleared in `onServerStopping`,
  because a client starts a new integrated server for each world it opens.
  - **Chat commands in singleplayer (decided)**: nodes made with `PermissionNodes.chatNode` (msg,
    reply, toggles, ignore, socialspy, helpop, mail, broadcast, mute) fail `require()` unless
    `permission.Multiplayer.active`: a dedicated server, or an integrated server opened to LAN.
    `Multiplayer.tick` re-sends the command tree when that changes. `registerCommands` gets
    `dedicated` from the loader (command selection), and `overrideVanilla` only applies when it is
    true, so singleplayer and LAN keep vanilla `/msg`.

### Multi-version with Stonecutter

- Minecraft API differences are handled with Stonecutter comments on the sources:
  `//? if >=1.20.5 {` … `//?} else {` … `//?}`. Use version-range conditions only, never
  loader conditions. Loader differences belong in the loader modules.
- Put version branches in **one place**: a small adapter class per topic in
  `common/.../compat/` (`Texts`, `Players`, `ItemData`, `ItemLookup`, `MovementSpeed`, …). Command code calls
  these adapters and contains no `//?` blocks itself.
- Vanilla class renames that would otherwise reach command code are Stonecutter replacements in
  `stonecutter.gradle.kts` (`ResourceLocation` → `Identifier`, `ClickType` → `ContainerInput`).
  Write the old name in a new version's sources only through those replacements: the active
  version's files on disk show the new name.
- Known version splits we need to handle: 1.19 (chat and `Component` API, the
  `CommandBuildContext` argument), 1.20 (`Level` vs `ServerLevel` getters, smithing menu),
  1.20.5 (NBT → data components, attribute modifier IDs), 1.21 (`ResourceLocation` factory
  methods), 1.21.5 (`CompoundTag` getters return Optionals: `compat/Tags`), 26.1 (saved data
  codecs and location, world clocks, `PermissionSet`, `NameAndId`, Forge EventBus 7 `BUS` fields).
- The source files on disk are always in the state of the **active** version. Before you
  commit, switch back to the VCS version (1.20.1) so that diffs stay readable.

### Commands

Register commands with Brigadier only. Put each command, or a small family of closely related
commands, in a class under `common/.../command/`. Each command class exposes a static
`register(dispatcher)` method that is called from `CmdEssentials.registerCommands`. Guard every
node with a `PermissionNodes` entry through `.requires(NODE.require())`. For an alias with no
arguments, copy `executes` onto the alias as well as setting `redirect`, because a bare Brigadier
redirect does nothing. `EssentialsCommand` shows the pattern.

## Server-side rules (hard constraints)

- Never reference client classes (`net.minecraft.client.*`, `@Environment(EnvType.CLIENT)`,
  `Dist.CLIENT`).
- Keep the mod optional on the client on both loaders, so vanilla clients aren't rejected (see
  **Code split → Sides**). Don't add any client entrypoint or client-only logic: on a client
  the mod only acts through the integrated server.
- Don't add custom items, blocks, entities, network packets, registries or resource-pack
  content. Anything a vanilla client can't render is not allowed.
- Don't use translation keys from this mod (`Component.translatable("cmdessentials.*")`): the
  client has no lang file for them. Build messages on the server from our own message files
  (see **Messages**). Vanilla translation keys are fine.

## Permissions

- Fabric: `fabric-permissions-api` (works with LuckPerms). Forge: `PermissionAPI`. How each
  loader wires this up is described under **Code split**.
- With no permission provider, fall back to an OP level per node (default: basic player
  commands = 0, admin commands = 2, `/sudo` = 4).
- Node format: `cmdessentials.<command>[.<sub>]`, e.g. `cmdessentials.home`,
  `cmdessentials.home.others`, `cmdessentials.speed.others`. Teleport bypasses:
  `cmdessentials.teleport.bypass.delay` / `.cooldown`.
- Any command that acts on another player needs its own `.others` node.
- Numeric per-player values are `IntOption`s, for example `cmdessentials.sethome.limit` (home
  count, -1 = unlimited). On Fabric that is a LuckPerms meta value (`Options.get`); on Forge it
  is an integer `PermissionNode`. Without a provider the config default applies.

## Data & config

- Persistent data is one `EssData` (`SavedData`, `format: 2`) on the overworld:
  `<world>/data/cmdessentials.dat`, or from 26.1
  `<world>/dimensions/minecraft/overworld/data/minecraft/cmdessentials.dat`. On 26.1+,
  `VersionedSavedData` reads the old file once when the new one doesn't exist. Server-wide: warps, kits and the spawn point. Per player,
  one `PlayerRecord`: homes, `/back`, teleport toggles, `/fly`, `/god`, last name, last seen,
  logout location, mail, nickname, message settings, ignore list, kit claims and mute. Locations
  are `StoredLocation`s: dimension id, position, yaw and pitch. `/tpa` requests, pending
  teleports, AFK state and session start times are in memory only.
- Config: `config/cmdessentials/config.json` (`EssConfig`), parsed with the Gson that ships
  with Minecraft. That's why it's JSON and not TOML: neither loader would need an extra
  library. Missing options are filled with defaults and written back. `/cmdessentials reload`
  re-reads the config and messages without a restart.
- Options follow EssentialsX `config.yml` (2.x): same meaning and default, in camelCase JSON
  groups, for example `heal-cooldown` → `heal.cooldownSeconds`, `spawn-if-no-home` →
  `homes.spawnIfNoHome`, `max-nick-length` → `nick.maxLength`. When adding a feature, look up
  its Essentials options first and don't hard-code them. Deliberate differences:
  `teleport.delaySeconds` defaults to 3 (Essentials: 0); `perPlayerLanguage` to true
  (Essentials: false). Options for Bukkit-only or unimplemented features aren't added until
  the feature exists. Test steps: `docs/testing/config-checklist.md`.
- Never block the server thread on file I/O while the server is running: the initial load
  happens during mod init, and reload reads the files on another thread
  (`ConfigManager.reload`) and swaps them in on the server thread.

## Messages

- Every user-facing string comes from `config/cmdessentials/messages_<lang>.json` and is turned
  into a `Component` on the server. `en_us` and `zh_cn` are bundled in
  `common/src/main/resources/cmdessentials/lang/` and written out on first run. New keys are
  merged into existing files. Admins can add more languages by dropping in another
  `messages_<lang>.json`.
- **Per-player language**: each player sees their client's language when a file for it exists.
  Otherwise the config's `language` applies, then `en_us`, then the raw key. The console uses
  `language`. `perPlayerLanguage: false` makes everyone use `language`.
- `Messages.info/error/raw` take the **recipient** (a `ServerPlayer` or `CommandSourceStack`),
  never a bare key, so no message is rendered without deciding whose language it is in. Use
  `CommandUtil.ok/fail/tell` in commands. Templates use `{0}`, `{1}`, …
- Style: gray body, gold arguments, red for errors, and an optional gold `prefix` key.
- Every key added to `en_us.json` must also be added to `zh_cn.json`.

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
  and drop the clashing aliases. Admins can opt in through the config list `overrideVanilla`;
  to override, remove the vanilla node from the dispatcher root before registering ours.
  **Exception (decided):** `overrideVanilla` defaults to `["msg", "tell", "w"]`, because `/r`,
  `/ignore` and `/socialspy` only work if private messages go through our command. Admins can
  empty the list to get vanilla messaging back; `/emsg` always exists.
- **Shortcuts for vanilla commands** (`command/VanillaAliases`, requested by the user): they
  only redirect to, or run, the real vanilla command through the dispatcher, so vanilla keeps
  its behaviour, feedback and permission check. They don't replace anything, so they're
  allowed by default. So far: `/gm` → `/gamemode`; `/gamemode` (and `/gm`) also accept
  `c s a sp` and `0 1 2 3` (merged into vanilla's node); `/gmc /gms /gma /gmsp [targets]`.
- **Players**: parse `[player]` with `EntityArgument`. Acting on another player needs the
  command's `.others` permission node.
- **`[player:]name`** (homes): lets admins manage other players' homes, under the
  `cmdessentials.home.others` node.
- **`datediff`** (`1d2h30m`): one shared parser, used by `/mute`, `/tempban`, `/togglejail`,
  `/mail sendtemp`, …
- **Items and blocks**: always use standard namespaced IDs (`minecraft:diamond`; a bare `diamond`
  is accepted as input) in commands, config and output (user requirement). Block lists belong
  in config as IDs (`teleport.unsafeBlocks`), not hard-coded. Look IDs up with `compat/ItemLookup`. There are no numeric
  IDs, no `:data` suffix and no Essentials `items.csv`. Item NBT changed to data components in
  1.20.5, so hide that difference behind a version helper.
- **"world"** in Essentials means a dimension here (`/world`, `/broadcastworld`, `/time … <world>`).
- **Container commands** (`/workbench`, `/anvil`, `/grindstone`, …): subclass the vanilla menu
  with a real `ContainerLevelAccess` and override `stillValid` (`menu/PortableMenus`). Never use
  `ContainerLevelAccess.NULL`: results aren't computed and items left inside are lost on close.
- All teleports go through one shared teleport service. It handles the delay, cancelling the
  teleport if the player moves, cooldowns, `/tptoggle`, saving the `/back` location, and
  moving between dimensions.

### P0: homes, spawn, teleport basics

- [x] `/sethome`, `/home`, `/delhome`, `/renamehome`: homes don't need a bed and work across
  dimensions. `/home` with no name lists your homes if you have more than one (Essentials has
  no `/homes`). The number of homes is limited by `homes.defaultLimit` or the
  `cmdessentials.sethome.limit` option. `player:name` works in all four for admins
  (`/renamehome` needs it quoted: `/renamehome "Steve:base" home2`).
- [x] `/setspawn`, `/spawn`: `/setspawn` sets the spawn and `/spawn` teleports to it, falling
  back to the world spawn if none is set. Options: respawn there after death and on first join.
- [x] `/speed [walk|fly] <multiplier> [player]`: with no type given, use `fly` while the player is
  flying and `walk` otherwise. `1` = vanilla default.
  - Fly: set `Abilities.flyingSpeed`, then call `onUpdateAbilities()`.
  - Walk: add or replace a transient `MOVEMENT_SPEED` attribute modifier. Don't use
    `Abilities.walkingSpeed`, which only changes the client's FOV.
  - Mapping: a nonnegative multiplier (`default * v`): 0.5 = half, 1 = vanilla, 2 = double.
    There is no command cap of 10. Vanilla movement attribute limits still apply.
    The old `speed.maxWalkMultiplier` / `speed.maxFlyMultiplier` settings are no longer used.
  - Known limit (decided: documented, not capped): vanilla's "moved too quickly" check pulls a
    player back when one move packet covers more than about 10 blocks per tick. Sprint-flying
    hits it from roughly fly 9-10, normal flying from about 18, walking only above about 30.
    The singleplayer host is exempt, so test high values on a dedicated server or as a LAN guest.
  - The change is not persisted: it resets on relog and respawn.
- [x] `/back`: your location before the last teleport. Optionally your death location too.
- [x] `/tpa`, `/tpahere`, `/tpaccept`, `/tpdeny`, `/tpacancel`, `/tptoggle`, `/tpauto`
- [x] `/warp`, `/setwarp`, `/delwarp`, `/warpinfo`
- [x] `/essentials` → `/cmdessentials` (alias `/ess`): `reload` and version info

### P1: player utilities, chat, inventory

Build P1 in milestones. Each milestone ends with all 6 jars building, a console smoke test, an
in-game checklist in `docs/testing/` and a commit.

**M1: shared infrastructure** (no new player-facing commands)

- [x] `PlayerRecord`: one record per player in `EssData` (`format: 2`) for all per-player
  state. Homes, `/back`, `/tptoggle` and `/tpauto` live there, plus last name, last seen time
  and logout location (recorded on leave). P0 files (no `format` key) are migrated on load;
  verified on real P0 data. New per-player state goes into `PlayerRecord`, changed only through
  `EssData` methods so it gets marked dirty.
- [x] `util.DateDiff`: `1y2mo3w4d5h6m7s`-style durations, parse and format.
- [x] `util.Pager`: 1-based pages; `/warp <page>` uses it (numbers up to 4 digits are pages).
- [x] `message.TextFiles`: `config/cmdessentials/{motd,rules,info}_<lang>.txt` parsed into
  `TextDocument`s (`#chapter` headings, `##` comments), in the player's language. en_us and
  zh_cn are bundled and written out once; they're never overwritten. Loaded and reloaded with
  the config.
- [x] JUnit 5 unit tests in `common/src/test` for the pure-Java helpers. Run with
  `./gradlew :common:1.20.1:test`; CI runs them too.
- Moved to the milestone that first needs them, so they're tested where they're used:
  the chat hook (M2 AFK), dimension-change hook (M2 fly), latency adapter (M3 `/ping`),
  `overrideVanilla` and the tab-list packet adapter (M5), `ItemData` (M6/M7).

**M2: player state**

- [x] `/fly`, `/god`, `/heal`, `/feed`, `/rest`, `/ext`, `/afk`, `/suicide`
  (`command/PlayerStateCommands`, `command/AfkCommand`, `player/AbilityService`,
  `player/AfkService`; checklist `docs/testing/m2-checklist.md`, played in-game on Fabric 1.20.1)
  - `/god` and `/fly` are stored in `PlayerRecord`. Instead of hooking every reset (respawn,
    gamemode change, join), `AbilityService` re-applies them once a second; turning one off
    rebuilds abilities from the gamemode first, so creative flight survives. God mode uses
    `Abilities.invulnerable` and keeps food full.
  - `/heal`: full health, food and saturation, no fire, removes harmful effects.
    `/rest`: resets the `TIME_SINCE_REST` stat (phantoms).
  - AFK (decided): after `afk.autoSeconds` (default 300) without moving, turning, chatting or
    running commands, the player is marked AFK and it is broadcast (`afk.broadcast`). Auto-kick
    is off by default (`afk.kickSeconds`; `cmdessentials.afk.kickexempt` is never kicked).
    Chat and commands are seen by `ServerGamePacketListenerImplMixin`, which runs on the
    network thread on 1.19+ and therefore only records a timestamp; state changes happen in
    `AfkService.tick`. The tab list shows the AFK state (see `Nicknames`).

**M3: info**

- [x] `/getpos`, `/compass`, `/depth`, `/near`, `/seen`, `/whois`, `/playtime`, `/ping`,
  `/motd`, `/rules`, `/info` (`command/LocationInfoCommands`, `PlayerInfoCommands`,
  `TextCommands`; checklist `docs/testing/m3-checklist.md`, played in-game on Fabric 1.20.1)
  - Player names resolve through `Players.findId`: online players, then names the mod recorded
    on join. **Never** use the vanilla profile cache for lookups: for unknown names it calls
    Mojang's API and blocks the server thread.
  - Offline `/playtime` reads `<world>/stats/<uuid>.json` off the server thread.
  - Text lines support `&` colour codes and `{player}`, `{online}`, `{max}`.
  - `/seen` and `/whois` read `PlayerRecord`. Only admins (`whois` node) see IPs.
  - `/ping` answers like Essentials ("Pong!") and adds the latency.
  - The MOTD is shown on join if `motd.showOnJoin` is set.

**M4: teleport extensions** (all through `TeleportService`)

- [x] `/tphere`, `/tppos`, `/tpo`, `/tpohere`, `/tpall`, `/tpaall`, `/tpoffline`, `/top`,
  `/bottom`, `/jump`, `/tpr`, `/settpr`, `/world` (`command/TeleportExtraCommands`,
  `teleport/SafeSpots`, `teleport/RandomTeleport`; checklist `docs/testing/m4-checklist.md`,
  played in-game on Fabric 1.20.1)
  - A player's own move (`/tppos`, `/top`, `/bottom`, `/jump`, `/tpr`, `/world`) goes through
    `TeleportService.teleport` (delay, cooldown); moving someone else uses `teleportNow`.
  - `SafeSpots`: sturdy, harmless floor and two free, dry blocks. It uses collision shapes and
    `isFaceSturdy`, because `blocksMotion()` doesn't exist on 1.18.2. Dimensions with a ceiling
    are scanned downwards from below the roof.
  - Commands that change the config (`/settpr`) validate first, then mutate
    `ConfigManager.get()` on the server thread and call `ConfigManager.save()`.
  - `/tphere` respects the target's `/tptoggle`; `/tpo` and `/tpohere` override it.
  - `/tpoffline` uses the logout location from `PlayerRecord`.
  - `/tpr`: random spot in a ring per dimension (`tpr.areas`: center, min and max range; only
    listed dimensions allow /tpr; the End isn't listed by default). Up to `tpr.attempts`
    tries, each loading one chunk on the server thread, so expect a short hitch in fresh land.
  - `/world [dimension]` (vanilla `DimensionArgument`): overworld → world spawn, End → spawn
    platform or the main island, others → the coordinate-scaled position at a safe height.
    Without an argument it switches between the overworld and the Nether.

**M5: messaging** (highest risk)

- [x] `/msg` (+ `/tell`, `/w` via `overrideVanilla`, always `/emsg`), `/r`, `/msgtoggle`,
  `/rtoggle`, `/ignore`, `/socialspy`, `/helpop`, `/mail`, `/broadcast`, `/broadcastworld`
- [x] `/nick`, `/realname` (`docs/testing/m5-checklist.md`; all six targets pass console smoke
  tests with mixin verification; played in-game on Fabric 1.20.1)
  - Nicknames need a common mixin on `Player#getDisplayName` and the tab-list name, plus a
    tab-list update packet when a nick changes (version-specific, see M1 compat).
  - `/ignore` (decided): blocks the ignored player's `/msg`, `/mail` and `/tpa` requests.
    Hiding their public chat needs a version-specific chat mixin (signed chat on 1.19+) and
    comes later as its own task.
  - `/mail` is persisted in `PlayerRecord`; `sendtemp` uses `DateDiff`. Unread mail is
    announced on join.

**M6: inventory and portable blocks**

- [x] Inventory: `/enderchest`, `/invsee`, `/hat`, `/repair`, `/more`, `/clearinventory`,
  `/clearinventoryconfirmtoggle`, `/disposal` (`command/InventoryCommands`, `menu/`; checklist
  `docs/testing/m6-checklist.md`; played in-game on Fabric 1.20.1)
  - `/invsee`: a 6-row chest menu backed by `InvseeContainer`, a live view of the target's 41
    slots; padding slots are locked. Without `invsee.modify` it's read-only. Online players only.
  - `/clearinventory`: `*` as first argument means every player; `*` as item means the main
    inventory, `**` adds armor and offhand. Items use `ResourceLocationArgument`, because
    `ItemArgument` needs `CommandBuildContext`.
- [x] Portable blocks: `/workbench`, `/anvil`, `/cartographytable`, `/grindstone`, `/loom`,
  `/smithingtable`, `/stonecutter`

**M7: kits**

- [x] `/kit`, `/createkit`, `/delkit`, `/showkit`, `/kitreset` (`command/KitCommands`,
  `data/Kit`, `compat/ItemData`; checklist `docs/testing/m7-checklist.md`, played in-game on Fabric 1.20.1)
  - Items are stored as serialised tags with standard IDs and decoded through `ItemData`.
    `/showkit` prints the IDs. `/createkit <name> <delay> [opLevel]` takes the creator's
    inventory. Delay: seconds, a `1d2h` duration, or -1 for one claim. Claims are kept in
    `PlayerRecord`; the `kitreset` node also skips the cooldown.
  - Permissions (decided): each kit stores a required OP level (default 0). With a permission
    provider, `cmdessentials.kits.<name>` also grants it. This avoids Forge's rule that nodes
    must be registered at startup.

### P2: admin, world, items

Build P2 in milestones like P1: M8 moderation (below), M9 jail, M10 `/vanish` and the
vanilla-clash versions, M11 per-player view and world tools, M12 items and power tools.

- [x] M8 moderation: `/mute`, `/tempban`, `/tempbanip`, `/unban`, `/unbanip`, `/kickall`,
  `/sudo`, `/gc` (`command/ModerationCommands`, `player/Mutes`; checklist
  `docs/testing/m8-checklist.md`; injections verified on all targets, core checks played
  in-game on 1.18.2, signed-chat `/mute` on 1.19+ still untested)
  - `/mute` cancels at `broadcastChatMessage` (1.19.3+) or the private
    `handleChat(FilteredText)` (before), never when the packet arrives: dropping a chat packet
    desyncs signed chat's last-seen tracking and gets the player kicked. It also blocks `/msg`
    and `/mail`; other commands keep working.
  - Bans use the vanilla ban lists, so `/banlist` and `/pardon` work with them.
  - `/sudo` defaults to OP level 4: it is as powerful as the console.
- [x] M9 jail: `/setjail`, `/deljail`, `/jails`, `/togglejail` (`/jail`, `/unjail`),
  `/jailedplayers` (`command/JailCommands`, `player/JailService`). Online players only.
  Adventure mode stands in for block protection; `TeleportService` refuses jailed players'
  own teleports. Other commands are not blocked yet.
- [x] M10: `/vanish` (`player/VanishService`, `TrackedEntityMixin`, `ChunkMapAccessor`): filtered
  in the entity tracker, plus tab-list removal. Not persisted. `/near` honours it; `/msg`, `/list`
  and tab completion don't yet.
- [x] M10 vanilla-clash versions: `/eban`, `/ebanip`, `/ekick`, `/ekill`, `/elist`, `/eme`,
  `/egamemode`, `/etime`, `/eweather`, `/egive`, `/etp`, `/eenchant` redirect to vanilla
  (`command/VanillaAliases`), so they have vanilla's syntax, not Essentials'.
- [x] M11 per-player view: `/ptime`, `/pweather` (`player/PlayerView`). Not persisted; re-sent
  after vanilla's periodic time broadcast.
- [x] M11 world: `/thunder`, `/lightning`, `/burn`, `/ice`, `/remove`, `/spawner` (no delay
  argument), `/spawnmob` (no mount syntax), `/tree`, `/bigtree` (grown from real saplings),
  `/break` (`command/WorldCommands`).
- [x] M12 items: `/item`, `/itemdb`, `/itemname`, `/itemlore`, `/exp`, `/skull`, `/condense`
  (`items.condense` table), `/editsign` (`set`, `clear`), `/unlimited`
  (`command/ItemCommands`). `/enchant` is `/eenchant` above.
- [x] M12 power tools: `/powertool`, `/powertoollist`, `/powertooltoggle`, right-click only
  (`player/ItemPowers`, `ServerPlayerGameModeMixin`).
- [x] M12 item meta: `/book`, `/firework`, `/potion` (`/elixer`) (`command/ItemMetaCommands`,
  `compat/Books`, `compat/Fireworks`, `compat/Potions`: NBT before 1.20.5, components after).
  Options use Essentials' `key:value` syntax with tab completion. `/book` unsigns your own
  written book (plain text) or signs a book and quill; `title`/`author` edit a written book.
  `/firework` takes dye names or `#RRGGBB`, shapes `small_ball large_ball star creeper burst`,
  effects `trail twinkle`, power 0-3, `fire` up to 10. `/potion` power is the level (1 = I),
  duration in seconds (default 60); a second effect of the same type replaces the first.

Test steps for M9-M12: `docs/testing/p2-checklist.md`. Console-verified on all targets, not yet
played in-game.

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
- `/recipe`: the vanilla recipe book already shows this.

## Build & run

Run Gradle with **JDK 21**: `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`. Gradle 8.12
can't run on the JDK 25 that is this machine's default `java`. The toolchain picks JDK 17, 21
or 25 for each version on its own (all are installed under `/Library/Java`).

| Goal | Command |
|---|---|
| Build all 8 jars into `build/libs/all/` | `./gradlew buildAndCollect` |
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
- **`:forge:1.21.1:runServer` and `:forge:26.3:runServer` don't work.** Architectury Loom
  assembles a broken module path for these Forge dev runs ("Module org.lwjgl... not found").
  Excluding LWJGL only moves the failure further into Forge's bootstrap. Test them on a real
  server instead: install it with the official installer
  (`forge-<mc>-<ver>-installer.jar --installServer`), put
  `build/libs/all/cmdessentials-forge-*+<mc>.jar` in `mods/`, and run it. The other 6
  loader/version pairs work with `runServer`. Forge checks the EULA before it loads mods.
- Test with a **vanilla client** of the same Minecraft version connected to the dev server.
  This also proves vanilla clients can still join. Never test in singleplayer only.
- From a script, you can pipe console commands into `runServer` through stdin (for example
  `cmdessentials`, then `stop`).
- **Dev servers don't exit after `stop`.** The Architectury Transformer dev launcher leaves a
  non-daemon tiny-remapper thread pool running. Wait for `All dimensions are saved`, then kill
  the process (Ctrl+C, or `pkill -f "<branch>/versions/<mc>/.gradle/architectury"`). This
  happens only in dev; production jars don't contain that launcher.
- Before finishing a task, run `./gradlew buildAndCollect`. A change that compiles on only the
  active version isn't done.
- CI: `.github/workflows/build.yml` runs `buildAndCollect` on pushes to `main` and on pull
  requests, with Temurin 17, 21 and 25, and uploads the 8 jars as an artifact. Gradle finds the JDKs
  through `org.gradle.java.installations.fromEnv=JAVA_HOME_17_X64,JAVA_HOME_21_X64,JAVA_HOME_25_X64`. If you
  change the Java versions, update that list too.
- Test servers on 1.18.2: take Fabric API from Modrinth. The aggregate `fabric-api` jars for
  1.18.2 on maven.fabricmc.net are ~5 KB stubs with no modules, and the mod then crashes with
  `NoClassDefFoundError: ...CommandRegistrationCallback`. Dev runs aren't affected.
- In zsh, write node paths with braces (`":fabric:${mc}:build"`). zsh reads `$mc:r` or
  `$mc:s` as a variable modifier and mangles the path.
- Unit tests: `./gradlew :common:1.20.1:test` (JUnit 5, `common/src/test`). Keep them free of
  Minecraft bootstrap so they run on the active version only. Anything that needs a running
  server is covered by console smoke tests and the `docs/testing/` checklists.
- Planned: GameTest-based tests for services that need a world (teleport, homes).
