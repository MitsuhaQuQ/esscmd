# M7 (kits) in-game test checklist

Same setup as [p0-checklist.md](p0-checklist.md): **A** is OP, **B** is not.

| # | Do | Expect |
|---|---|---|
| 1.1 | A: fill the inventory with enchanted, named and damaged items, `/createkit starter 60` | `Kit starter created with N item stack(s).` |
| 1.2 | B: `/kit` | Lists `starter` |
| 1.3 | B: `/showkit starter` | Shows IDs: `minecraft:diamond x3, ...` |
| 1.4 | B: `/kit starter` | Gets every item; enchantments, names and damage intact |
| 1.5 | B: `/kit starter` again | Cooldown message with the time left; works again after 60 s |
| 1.6 | B: fill the inventory, claim a kit | What doesn't fit drops at B's feet; nothing lost |
| 1.7 | A: `/createkit once -1`; B claims twice | Second claim: "can only be claimed once" |
| 1.8 | A: `/createkit vip 1d 2`; B: `/kit`, `/kit vip` | Not listed for B; "no access" |
| 1.9 | A: `/kit starter B`, `/kitreset starter B` | B gets it regardless of cooldown; cooldown cleared |
| 1.10 | A: `/delkit starter`; B: `/kit starter` | "not found" |
| 1.11 | Restart the server | Kits and cooldowns remain |
| 1.12 | Add `minecraft:grass_block` to `teleport.unsafeBlocks`, reload, B: `/tpr` several times | Never lands on grass blocks |
