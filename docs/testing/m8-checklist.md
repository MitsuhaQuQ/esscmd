# M8 (moderation) in-game test checklist

Same setup as [p0-checklist.md](p0-checklist.md): **A** is OP, **B** is not. Use a client with
secure chat on. For `/sudo`, A needs OP level 4 (the default for `op`).

| # | Do | Expect |
|---|---|---|
| 1.1 | A: `/mute B 1m spam` | B is told; B's chat isn't shown to anyone and B sees "You are muted." |
| 1.2 | B: chat several times, then `/msg A hi`, `/mail send A hi` | All blocked; **B is not kicked** ("chat validation failed" would be a bug) |
| 1.3 | B: run commands such as `/home` while muted | Commands still work |
| 1.4 | Wait 1 minute, B: chat | Works again, no kick, other players see it |
| 1.5 | A: `/mute B`, then `/mute B` again | Muted until further notice, then unmuted |
| 1.6 | Mute B, restart the server | Still muted |
| 2.1 | A: `/tempban B 1m griefing` | B is kicked with the reason and length |
| 2.2 | B: reconnect | Refused with the ban screen; vanilla `/banlist` shows B |
| 2.3 | A: `/unban B`; B reconnects | Allowed |
| 2.4 | A: `/tempban B 10s`, wait, reconnect | Allowed after expiry |
| 2.5 | A: `/tempban B nonsense` | "Invalid duration" |
| 3.1 | A: `/tempbanip B 1m` from another machine, `/unbanip <ip>` | B kicked by IP, then allowed |
| 4.1 | A: `/kickall maintenance` | Everyone but A is kicked with that text |
| 4.2 | A: `/sudo B home` | B teleports home |
| 4.3 | A: `/gc` | Uptime, TPS, memory, chunks and entities per dimension |
