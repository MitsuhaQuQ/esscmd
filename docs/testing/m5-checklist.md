# M5 (messaging) in-game test checklist

Same setup as [p0-checklist.md](p0-checklist.md): **A** is OP with an English client, **B** is
not OP with a 简体中文 client. Add **C** when a third player is needed. Start with the default
`overrideVanilla` (`msg`, `tell`, `w`).

## Private messages and toggles

| # | Do | Expect |
|---|---|---|
| 1.1 | A: `/msg B hello`; B: `/r 你好` | Both messages arrive in the recipient's language; `/r` returns to A |
| 1.2 | Repeat with `/tell`, `/w`, `/emsg`, `/m`, `/t`, `/pm` | Every alias uses the same private-message system |
| 1.3 | B: `/msgtoggle off`; A messages B | Refused; B receives nothing. `/msgtoggle on` restores messages |
| 1.4 | B: `/rtoggle on`; B messages C, A messages B, then B: `/r test` | The reply goes to C (last recipient), not A (last sender) |
| 1.5 | B: `/rtoggle off`; repeat | The reply goes to the last sender |
| 1.6 | B: `/ignore A`; A tries `/msg`, `/mail send` and `/tpa B` | All three are refused; `/ignore A` again unignores A |
| 1.7 | A: `/socialspy on`; B and C exchange `/msg` and `/mail send` | A sees spy lines; neither B nor C sees them |
| 1.8 | B: `/helpop help me` | Online players with `cmdessentials.helpop.receive` see the request |

## Mail

| # | Do | Expect |
|---|---|---|
| 2.1 | A: `/mail send B persistent mail`; B: `/mail`, then `/mail read` | One unread mail is listed with sender and age, then marked read |
| 2.2 | Restart before B reads another mail; B rejoins | Unread count is announced and the mail is still present |
| 2.3 | A: `/mail sendtemp B 5s expiring`; wait 6 s; B: `/mail read` | The temporary mail is gone |
| 2.4 | B has several messages: `/mail clear 1`, then `/mail clear` | The selected message, then all remaining messages, are removed |
| 2.5 | A: `/mail clear B [number]` | A can remove B's mail; B cannot clear another player's mail |
| 2.6 | A: `/mail sendall server notice`; an offline known player rejoins | Online and offline known players receive the persisted mail |

## Broadcasts and nicknames

| # | Do | Expect |
|---|---|---|
| 3.1 | A: `/broadcast hello` | Everyone sees the localized broadcast; console is also logged |
| 3.2 | Put A and B in different dimensions; A: `/broadcastworld minecraft:overworld hello` | Only players in the overworld see it |
| 3.3 | A: `/nick A GoldA` | A's display name and tab-list name change immediately |
| 3.4 | A messages B after changing nick | B sees `GoldA`; social spy still identifies both account names |
| 3.5 | A: `/realname GoldA` | Reports A's account name |
| 3.6 | A: `/nick A off` | The account name returns immediately in display and tab list |
| 3.7 | B becomes AFK and returns | `[AFK]` appears and disappears in the tab list |

## Configuration and server-only check

| # | Do | Expect |
|---|---|---|
| 4.1 | Set `overrideVanilla` to `[]`, restart, use `/msg` and `/emsg` | `/msg` is vanilla; `/emsg` remains cmdEssentials |
| 4.2 | Restore defaults and restart | `/msg`, `/tell`, `/w` use cmdEssentials again |
| 4.3 | Join with an unmodified vanilla client | Join succeeds; nicknames and AFK tags need no client mod |

## Automated and console checks completed

- `./gradlew buildAndCollect`: all six jars built.
- Fabric 1.20.1 dev server with forced mixin target loading and mixin export: startup passed.
- Console syntax checks passed for `/msg`, `/mail`, `/nick`, `/broadcast` and `/broadcastworld`.
