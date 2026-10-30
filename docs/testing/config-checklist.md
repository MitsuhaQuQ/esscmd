# Essentials-based config options: in-game test checklist

Set each option in `config/cmdessentials/config.json`, run `/cmdessentials reload` (restart for
`disabledCommands`) and check. Defaults come from EssentialsX `config.yml`.

| # | Option | Test | Expect |
|---|---|---|---|
| 1 | `heal.cooldownSeconds` | Non-exempt player: `/heal` twice | Second refused; OPs bypass with `heal.cooldown.bypass` |
| 2 | `heal.removeEffects: false` | `/heal` while poisoned | Poison stays |
| 3 | `homes.spawnIfNoHome` | New player: `/home` | Goes to the spawn; with `false`, an error |
| 4 | `homes.confirmOverwrite: true` | `/sethome x` twice on an existing home | First asks, second replaces |
| 5 | `spawn.respawnAtHome: true` | Die with a home, no bed | Respawn at home; with a bed, at the bed unless `respawnAtHomeBed: false` |
| 6 | `teleport.waterIsSafe: true` | `/top` over the ocean | Lands in the water instead of an error |
| 7 | `teleport.toCenter: false` | `/top` | Lands at the block corner |
| 8 | `tpa.maxRequests: 1` | Two players `/tpa` the same target | Only the newest stays |
| 9 | `nick.*` | `/nick` with a long, invalid or blacklisted name | Refused; a valid nick shows the `~` prefix; `/realname` accepts `~name` |
| 10 | `messaging.replyTimeoutSeconds` | With `/rtoggle` on, message A, wait past the timeout, `/r` | Replies to the last sender instead of A |
| 11 | `mail.perMinute: 1` | Send two mails in a minute | Second refused |
| 12 | `moderation.maxMuteSeconds: 60` | Without `mute.unlimited`: `/mute B 1h` and `/mute B` | Both refused |
| 13 | `god.disabledDimensions`, `god.removeOnDisconnect` | Enter a listed dimension; relog | God has no effect there; relog clears it |
| 14 | `afk.cancelOnMove: false`, `afk.cancelOnChat: false` | `/afk`, then move or chat | Stays AFK |
| 15 | `warps.perWarpPermission: true` | Non-OP: `/warp` | Warps without `cmdessentials.warps.<name>` are hidden and refused |
| 16 | `kits.dropItemsIfFull` | Claim a kit with a full inventory | Refused by default; `true` drops the rest |
| 17 | `kits.skipUsedOneTimeKits: true` | Claim a `-1` kit, `/kit` | No longer listed |
| 18 | `items.defaultStackSize: 16` | `/more` | Stack of 16 |
| 19 | `items.repairEnchanted: false` | `/repair` an enchanted tool | Refused |
| 20 | `disabledCommands: ["gc"]` + restart | `/gc` | Unknown command |
