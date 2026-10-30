# M4 (teleport extensions) in-game test checklist

Same setup as [p0-checklist.md](p0-checklist.md): **A** is OP with an English client, **B** is
not OP with a 简体中文 client. Everything except `/tpr` is OP-only by default. A's own moves
are instant because OPs skip the teleport delay; B's `/tpr` waits 3 s.

## Moving players

| # | Do | Expect |
|---|---|---|
| 1.1 | A: `/tphere B` | B arrives at A at once; A: `Brought B to you.` |
| 1.2 | B: `/tptoggle off`; A: `/tphere B` | Refused: "B doesn't accept teleport requests" |
| 1.3 | Still toggled off. A: `/tpohere B`, then `/tpo B` | Both work (override /tptoggle). B: `/tptoggle on` afterwards |
| 1.4 | A: `/tpo B A` (with a third player C if available) | B is moved to A |
| 1.5 | A: `/tppos 100 80 100` | A at 100, 80, 100 keeping the facing direction |
| 1.6 | A: `/tppos ~ ~10 ~ 90 0` | 10 blocks up, facing west, level |
| 1.7 | A: `/tppos 0 70 0 0 0 minecraft:the_nether` | In the Nether at 0, 70, 0 |
| 1.8 | A: `/tpall` | B (and everyone else) is brought to A |
| 1.9 | A: `/tpaall` | B gets the Chinese request with [接受] [拒绝]; accepting brings B to A after 3 s |
| 1.10 | B logs out somewhere; A: `/tpoffline B` | A arrives where B logged out |
| 1.11 | A: `/back` after any of these | Returns to where A was before |

## Finding ground

| # | Do | Expect |
|---|---|---|
| 2.1 | A underground in a cave: `/top` | On the surface above, not in water or on leaves |
| 2.2 | A on the surface over a cave: `/bottom` | On the lowest safe floor below |
| 2.3 | A over the ocean: `/top` | "No safe place to stand was found there." |
| 2.4 | A in the Nether: `/top` | A safe spot below the bedrock roof, never on top of it |
| 2.5 | A: look at a distant block (up to 120 blocks), `/jump` | Standing on top of that block |
| 2.6 | A: look at the sky, `/jump` | "You are not looking at a block within range." |

## Random teleport

| # | Do | Expect |
|---|---|---|
| 3.1 | B: `/tpr` (overworld) | After 3 s, somewhere 200–5000 blocks from 0, 0, on safe ground |
| 3.2 | B: repeat `/tpr` a few times | Different places each time; never in water, lava or a tree |
| 3.3 | B in the Nether: `/tpr` | Inside the Nether, below the roof, not in lava |
| 3.4 | B in the End: `/tpr` | "Random teleport is not enabled in minecraft:the_end." |
| 3.5 | A: `/settpr minecraft:overworld` | Shows center and ranges |
| 3.6 | A: stand somewhere, `/settpr minecraft:overworld center`, `/settpr minecraft:overworld maxrange 300` | Saved; B's `/tpr` now lands within 300 blocks of that spot; still true after a restart |
| 3.7 | A: `/settpr minecraft:overworld minrange 500` (max 300) | Refused; ranges unchanged |
| 3.8 | Note the server TPS while running `/tpr` in fresh land | A short hitch is expected (chunk generation); no long freeze |

## /world

| # | Do | Expect |
|---|---|---|
| 4.1 | A in the overworld: `/world` | To the Nether at your x/8, z/8, on a safe floor |
| 4.2 | A in the Nether: `/world` | Back to the overworld world spawn |
| 4.3 | A: `/world minecraft:the_end` before anyone entered the End | On the main island, not in the void |
| 4.4 | B: `/world`, `/top`, `/tphere A` | Unknown commands (not OP) |
