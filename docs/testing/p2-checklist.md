# P2 M9-M12 in-game test checklist

Same setup as [p0-checklist.md](p0-checklist.md): **A** is OP, **B** is not.

## Jail

| # | Do | Expect |
|---|---|---|
| 1.1 | A: `/setjail cell`, `/jail B cell 1m` | B is moved there, in adventure mode, told the length |
| 1.2 | B: walk more than 8 blocks away; `/spawn` | Pulled back within a second; teleport refused |
| 1.3 | B: break a block | Not possible |
| 1.4 | Wait 1 minute | B returns to the old spot and gamemode |
| 1.5 | A: `/jail B cell`, `/unjail B`, `/jailedplayers` | Freed; the list follows |
| 1.6 | Jail B, restart, B rejoins | Still jailed |

## Vanish

| # | Do | Expect |
|---|---|---|
| 2.1 | A: `/vanish` near B (B without `vanish.see`, so no OP) | A disappears from B's world and tab list at once |
| 2.2 | A: walk around B, change dimension and return | B never sees A; no client errors |
| 2.3 | B relogs while A is vanished | A isn't in B's tab list |
| 2.4 | A: `/vanish off` | A reappears for B |
| 2.5 | With a second OP | They still see A |

## Vanilla `e` names and per-player view

| # | Do | Expect |
|---|---|---|
| 3.1 | `/eban`, `/ekick`, `/egive`, `/etp`, `/etime`, `/eenchant` | Same as vanilla |
| 3.2 | A: `/ptime night`, `/ptime 4pm B`, `/ptime reset` | Only those clients change; server time doesn't; stays for minutes |
| 3.3 | A: `/pweather storm`, `/pweather reset` | Only A's client rains |

## World

| # | Do | Expect |
|---|---|---|
| 4.1 | `/thunder true 30`, `/thunder false` | Storm, then clear |
| 4.2 | `/lightning` at a block, `/lightning B`, `/burn B 3`, `/ice B` | Each hits as described |
| 4.3 | `/remove drops 20`, `/remove monsters` | Counts shown, entities gone; players unaffected |
| 4.4 | Look at a spawner: `/spawner minecraft:zombie` | Spawner shows and spawns zombies |
| 4.5 | `/spawnmob minecraft:cow 3`, `/spawnmob minecraft:cow 100` | 3 cows; capped at 10 |
| 4.6 | On grass: `/tree birch`, `/bigtree darkoak` | Trees grow; on stone: "A tree cannot grow there", no saplings left |
| 4.7 | `/break` at stone, at bedrock | Breaks; bedrock refused below OP 4 |

## Items

| # | Do | Expect |
|---|---|---|
| 5.1 | `/item diamond 70`, `/itemdb` | Two stacks; shows the id, stack size, durability |
| 5.2 | `/itemname &aSword`, `/itemlore add &7Line`, `/itemlore set 1 X`, `/itemlore clear` | Name and lore change and survive relog |
| 5.3 | `/exp show`, `/exp give B 100`, `/exp set B 0` | Values update |
| 5.4 | `/skull Notch` | Head shows the skin |
| 5.5 | Hold 20 iron ingots, `/condense` | 2 blocks, 2 ingots left |
| 5.6 | `/recipe minecraft:chest` | Lists ingredient ids |
| 5.7 | Look at a sign: `/editsign set 1 &cHello`, `/editsign clear 1` | Text changes for everyone |
| 5.8 | `/unlimited minecraft:torch`, place torches | Stack refills within a second |
| 5.9 | Hold a stick, `/powertool home`, right-click air and a block | Runs `/home`; `/powertool` removes; `/powertooltoggle` disables |
