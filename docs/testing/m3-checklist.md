# M3 (info) and gamemode shortcuts: in-game test checklist

Same setup as [p0-checklist.md](p0-checklist.md): **A** is OP with an English client, **B** is
not OP with a 简体中文 client.

## Gamemode shortcuts

| # | Do | Expect |
|---|---|---|
| 1.1 | A: `/gmc`, `/gms`, `/gma`, `/gmsp` | Each switches A's mode with the vanilla "Set own game mode" message |
| 1.2 | A: `/gmc B` | B is creative; vanilla feedback for both |
| 1.3 | A: `/gamemode c`, `/gamemode 0`, `/gm sp`, `/gm 2` | creative, survival, spectator, adventure |
| 1.4 | A: `/gamemode creative`, `/gm survival B` | The vanilla full names still work |
| 1.5 | A: `/gms @a` | Everyone switches to survival |
| 1.6 | B: `/gmc`, `/gm c`, `/gamemode c` | All refused like vanilla (not OP) |
| 1.7 | A: type `/gamemode ` and look at the suggestions | Shows `c s a sp 0 1 2 3` and the full names |

## Location

| # | Do | Expect |
|---|---|---|
| 2.1 | B: `/getpos` | Own position, dimension, yaw and pitch, in Chinese |
| 2.2 | A: `/getpos B` | B's position. B: `/getpos A` is refused |
| 2.3 | A: face north (F3 shows "north"), `/compass` | `Bearing: N (0°)`; face east → `E (90°)` |
| 2.4 | A at y=63, then y=80, then y=40: `/depth` | "at sea level", "17 blocks above", "23 blocks below" |
| 2.5 | A near B (within 200 blocks): `/near` | `Players within 200 blocks: B (12m)` |
| 2.6 | A: `/near 5` while B is farther | "No players within 5 blocks" |
| 2.7 | A: `/near B 50` | Players near B. B: `/near` is refused |

## Player info

| # | Do | Expect |
|---|---|---|
| 3.1 | A: `/seen B` while B is online | "B is online, for 3m" |
| 3.2 | B logs out; A: `/seen B` | "B was last seen 10s ago" |
| 3.3 | A: `/seen Notch` (never joined) | "has never been seen on this server", instantly (no lag) |
| 3.4 | A: `/whois B` (online) | UUID, online time, location, health/food, gamemode, OP/fly/god/AFK, IP |
| 3.5 | A: `/whois B` (offline) | Last seen, logout location, fly/god |
| 3.6 | B: `/whois A` | Refused (not OP) |
| 3.7 | B: `/playtime`, A: `/playtime B` | Play time like `2h 13m 5s`. Also works for offline B |
| 3.8 | B: `/ping` | `Pong! 你的延迟是 23 毫秒。` (real latency) |

## Texts

| # | Do | Expect |
|---|---|---|
| 4.1 | B joins | The Chinese MOTD with B's name and the online count |
| 4.2 | A: `/motd`, `/rules` | English texts |
| 4.3 | A: `/info`, `/info commands`, `/info nope` | Intro, the commands chapter, "There is no chapter nope." |
| 4.4 | Add 12 lines to `rules_en_us.txt` with `&a` colour codes, `/cmdessentials reload`, A: `/rules` | Green text; page 1 of 2 with a hint to `/rules 2`; `/rules 2` shows the rest |
| 4.5 | Set `motd.showOnJoin` to false, reload, rejoin | No MOTD on join; `/motd` still works |
