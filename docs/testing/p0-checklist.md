# P0 in-game test checklist

Run this checklist against a real server with real clients. Console tests can't cover any of it.

## Setup

- Server: `testserver/1.20.1-fabric/` (git-ignored). Start it with `./start.sh`. It needs
  `eula=true` in `eula.txt` first.
- Two players, connected with vanilla 1.20.1 clients:
  - **A**: the admin. Run `op A` in the server console. Client language: English.
  - **B**: an ordinary player, not OP. Client language: 简体中文 (Options → Language).
- The defaults in `config/cmdessentials/config.json` apply: 3 s teleport delay, cancel on
  move, 3 homes, 120 s `/tpa` timeout.

Mark each line ✅ or ❌. For ❌, note what happened and any console error.

## 0. Server-only and language

| # | Do | Expect |
|---|---|---|
| 0.1 | Join with both vanilla clients | Both join; no "mod missing" or "incompatible" screen |
| 0.2 | B: `/home` | Chinese: `你还没有设置家。请使用 /sethome。` |
| 0.3 | A: `/home` | English: `You haven't set a home yet. Use /sethome.` |
| 0.4 | B: switch the client to English, then `/home` | Now English. The language follows the client live |

## 1. Homes

| # | Do | Expect |
|---|---|---|
| 1.1 | B: `/sethome` | `Home home set.` (in B's language) |
| 1.2 | B: walk away, `/home` and stand still | "Teleporting in 3 seconds" → teleported to the home after 3 s |
| 1.3 | B: `/home`, then move during the 3 s | "Teleport cancelled because you moved", no teleport |
| 1.4 | B: `/sethome base`, `/sethome mine` | Both set (3 homes in total) |
| 1.5 | B: `/sethome fourth` | Refused: limit is 3 |
| 1.6 | B: `/home` (no name, several homes) | Lists `base, home, mine`; no teleport |
| 1.7 | B: `/home ` then Tab | Suggests B's homes |
| 1.8 | B in the Nether: `/sethome nether` (refused: limit), `/delhome mine`, `/sethome nether`. Back in the overworld: `/home nether` | Lands in the Nether at that spot |
| 1.9 | B: `/renamehome base base2`, `/home base2` | Renamed; teleport works |
| 1.10 | B: `/home A:` | Refused: no permission for other players |
| 1.11 | A: `/home B:` | Lists B's homes. `/home B:base2` teleports A there **instantly** (admin skips delay) |
| 1.12 | B: `/sethome bad name!` | Invalid-name error |

## 2. Spawn

| # | Do | Expect |
|---|---|---|
| 2.1 | B: `/spawn` before any `/setspawn` | Teleports to the world spawn |
| 2.2 | B: `/setspawn` | No such command for B (not OP) |
| 2.3 | A: stand somewhere distinctive, `/setspawn` | `Spawn set at x, y, z (minecraft:overworld)` |
| 2.4 | B: `/spawn` | Lands at A's spot, facing A's direction |
| 2.5 | A: `/spawn B` | B teleported instantly; A gets `Sent B to spawn.` |
| 2.6 | B (no bed): die (e.g. `/kill` by A) and respawn | Respawns at the `/setspawn` point |
| 2.7 | B: sleep in a bed, then die and respawn | Respawns at the bed, not at the spawn |

## 3. Back

| # | Do | Expect |
|---|---|---|
| 3.1 | B: note the position, `/spawn`, then `/back` | Returns to the noted position |
| 3.2 | B: die somewhere, respawn, `/back` | Returns to the death spot |
| 3.3 | A: `/back B` | B goes to their last /back location instantly |

## 4. TPA

| # | Do | Expect |
|---|---|---|
| 4.1 | B: `/tpa A` | B: "Request sent". A: "B wants to teleport to you" + green **[Accept]** red **[Deny]** + "Expires in 120 seconds" |
| 4.2 | A: hover, then click **[Accept]** | Hover shows `/tpaccept B`. B gets "A accepted"; B teleports to A after 3 s |
| 4.3 | B: `/tpahere A`; A: `/tpaccept` | A is brought to B (A is admin, so instantly) |
| 4.4 | B: `/tpa A`; A clicks **[Deny]** | B: "A denied your teleport request" (in Chinese) |
| 4.5 | B: `/tpa A`, then B: `/tpacancel` | B: "Cancelled 1 teleport request(s)"; A is told B cancelled |
| 4.6 | A: `/tptoggle`; B: `/tpa A` | B: "A doesn't accept teleport requests". A: `/tptoggle` again turns it back on |
| 4.7 | A: `/tpauto on`; B: `/tpa A` | Accepted automatically; B teleports. A: `/tpauto off` |
| 4.8 | B: `/tpa B` (self) | Refused |
| 4.9 | Optional: set `tpa.timeoutSeconds` to 10, `/cmdessentials reload`, B: `/tpa A`, wait | After 10 s B gets "Your teleport request to A expired" |
| 4.10 | B: `/tpa A`, then B disconnects; A: `/tpaccept` | A: "no pending requests" (the request was dropped) |

## 5. Warps

| # | Do | Expect |
|---|---|---|
| 5.1 | B: `/warp` | "There are no warps." |
| 5.2 | A: `/setwarp shop` somewhere | `Warp shop set.` |
| 5.3 | B: `/warp`, `/warp shop` (Tab completes) | Lists `shop`; teleports after 3 s |
| 5.4 | B: `/warpinfo shop` | Shows coordinates and dimension |
| 5.5 | B: `/setwarp x`, `/delwarp shop` | No such commands for B |
| 5.6 | A: `/warp shop B` | B teleported instantly |
| 5.7 | A: `/delwarp shop`; B: `/warp shop` | "Warp shop not found." |

## 6. Speed (A only; it is an admin command)

| # | Do | Expect |
|---|---|---|
| 6.1 | A on the ground: `/speed 3` | `Walking speed set to 3.0 for A.` Clearly faster walking |
| 6.2 | A flying (creative): `/speed 5` | `Flying speed set to 5.0`, faster flight. Walking unchanged |
| 6.3 | A: `/speed walk 1`, `/speed fly 1` | Back to vanilla speeds |
| 6.4 | A: `/speed walk 0` | Can't walk or sprint (jumping still works) |
| 6.5 | A: `/speed walk 2 B` | B walks faster |
| 6.6 | B: `/speed 2` | No such command for B |
| 6.7 | A: `/speed walk 5`, relog | Speed is back to normal after relog (by design) |

## 7. Persistence and reload

| # | Do | Expect |
|---|---|---|
| 7.1 | Console: `stop`, start the server again | Homes, warps, spawn, `/back`, `/tptoggle` and `/tpauto` all still there |
| 7.2 | Edit `messages_zh_cn.json`, change `home.none`, then `/cmdessentials reload` | B sees the edited text without a restart |
| 7.3 | Set `teleport.delaySeconds` to 0, reload, B: `/spawn` | Instant teleport |
| 7.4 | Break `config.json` (delete a brace), reload | Error message with the JSON problem; server keeps running on the old config |
| 7.5 | New player C joins for the first time (if available) | Lands at the `/setspawn` point |
