# M2 (player state) in-game test checklist

Same setup as [p0-checklist.md](p0-checklist.md): **A** is OP with an English client, **B** is
not OP with a 简体中文 client. Put the new jar in `mods/` and restart. To get fast AFK tests,
set `afk.autoSeconds` to 20 and `afk.kickSeconds` to 40 in `config/cmdessentials/config.json`,
then run `/cmdessentials reload`.

## /fly and /god

| # | Do | Expect |
|---|---|---|
| 1.1 | A (survival): `/fly` | `Flight enabled for A.` Double-jump starts flying |
| 1.2 | A: `/gamemode creative`, then `/gamemode survival` | A can still fly within a second of switching back |
| 1.3 | A: die and respawn, relog | A can still fly after each |
| 1.4 | A in the air: `/fly off` | Flight stops; A falls (fall damage applies) |
| 1.5 | A (creative): `/fly off` | Creative flight still works (only the extra flag is removed) |
| 1.6 | A: `/god`, then take damage from a mob, lava, a fall | No damage, hunger stays full |
| 1.7 | A: `/god off` in survival | Damage works again |
| 1.8 | A: `/fly B on`, `/god B` | B gets the Chinese message `已为 B 开启飞行。`; B can fly and is invulnerable |
| 1.9 | B: `/fly`, `/god` | Unknown command (not OP) |
| 1.10 | Restart the server | A's and B's fly and god are still on |

## Heal, feed, rest, ext, suicide

| # | Do | Expect |
|---|---|---|
| 2.1 | A: take damage, get poisoned (spider or potion), set yourself on fire, `/heal` | Full health and food, fire out, poison gone; a Speed potion stays |
| 2.2 | A: get hungry, `/feed` | Food bar full |
| 2.3 | A: `/heal B`, `/feed B`, `/ext B` | Each works; B also sees the message in Chinese |
| 2.4 | A: `/rest` | Message shown. Optional: stay awake 3+ nights first, then `/rest` and no phantoms spawn |
| 2.5 | B: `/suicide` | B dies with the normal death message |
| 2.6 | B: `/heal` | Unknown command |

## AFK

| # | Do | Expect |
|---|---|---|
| 3.1 | B: `/afk` | Everyone sees `B is now AFK.` (A in English, B in Chinese) |
| 3.2 | B: move | Everyone sees `B is no longer AFK.` |
| 3.3 | B: `/afk brb dinner` | `B is now AFK: brb dinner` |
| 3.4 | B: send a chat message | B is no longer AFK |
| 3.5 | B: `/afk`, then only turn the camera | B is no longer AFK |
| 3.6 | B: stand still 20 s (`afk.autoSeconds` = 20) | B goes AFK automatically |
| 3.7 | B: keep standing still until 40 s | B is kicked with `你因暂离时间过长被踢出。` |
| 3.8 | A: stand still 40 s | A goes AFK but isn't kicked (OP has `afk.kickexempt`) |
| 3.9 | A: `/afk B` | B's AFK state toggles |
| 3.10 | B: `/afk A` | Not a toggle for A: B goes AFK with the message `A` |
| 3.11 | Set `afk.broadcast` to false, reload, B: `/afk` | Only B is told |
