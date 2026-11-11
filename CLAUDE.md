# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

The full project guide, covering architecture, rules, the command roadmap and build instructions, is
`AGENTS.md`. Read it before changing code; it is imported here:

@AGENTS.md

## Quick reference

Reply to the user in Chinese for the whole session and keep it constant: progress notes,
summaries and questions included, even when tool output or files are in English. Write code,
comments and commit messages in English.

| Goal | Command |
|---|---|
| Build all 8 jars (required before finishing any task) | `./gradlew buildAndCollect` → `build/libs/all/` |
| Unit tests (active version only) | `./gradlew :common:1.20.1:test` |
| One test class | `./gradlew :common:1.20.1:test --tests "com.mtmrails.esscmd.util.DateDiffTest"` |
| Dev server for the active version | `./gradlew runActiveServerFabric` / `runActiveServerForge` |
| Switch version / back to VCS version (1.20.1) | `./gradlew "Set active project to 1.21.1"` / `./gradlew "Reset active project"` |

There is no linter; compiling all eight targets through `buildAndCollect` is the check.

The big picture, in short:
- `common` holds all logic (Mojmap + Brigadier only). `fabric` and `forge` only forward loader events
  to the hooks on `CmdEssentials` and implement `platform.Platform`.
- Minecraft version differences (1.18.2, 1.20.1, 1.21.1, 26.3) live only in `common/.../compat/` as Stonecutter `//? if` blocks.
  Files on disk are in the state of the active version, so reset to 1.20.1 before committing.
- Server-side only: no client classes, no custom content and no mod translation keys. The jar may also
  load on a client for singleplayer through the common entrypoint.

## Building on this Windows machine

AGENTS.md describes the macOS setup. Here:
- Run Gradle on JDK 21 (`JAVA_HOME='C:\Program Files\Microsoft\jdk-21.0.13.21-hotspot'`); the
  default `java` is JDK 25. Point the toolchain at the JDKs next to it with `JAVA_HOME_17_X64`,
  `JAVA_HOME_21_X64` and `JAVA_HOME_25_X64` (same folder, `jdk-17.0.21.21-hotspot`,
  `jdk-21.0.13.21-hotspot`, `jdk-25.0.5.21-hotspot`), as CI does.
- Gradle may fail with `java.io.IOException: Unable to establish loopback connection`. If it does,
  point the temp dirs at a local folder:
  `TEMP`/`TMP` plus `JAVA_TOOL_OPTIONS="-Djdk.net.unixdomain.tmpdir=<dir> -Djava.io.tmpdir=<dir>"`.
  It also has to run outside the command sandbox. A first cold build takes several minutes.
- `F:\MC\esscmd-gradle-tmp` (outside the repo) works as that folder; a path under
  `%LOCALAPPDATA%\Temp` did not. Don't put it inside the repo: it isn't in `.gitignore`.
- A local MultiMC test instance for Fabric 1.20.1 singleplayer is at
  `N:\Games\MultiMC\instances\1.20.1_Worldgen\.minecraft` (mods in `mods/`, log in `logs/latest.log`).
