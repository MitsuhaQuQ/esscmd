# M6 (inventory and portable blocks) in-game test checklist

Same setup as [p0-checklist.md](p0-checklist.md): **A** is OP, **B** is not. Only `/disposal`
is open to everyone by default.

## Portable blocks (the main risk: lost or duplicated items)

| # | Do | Expect |
|---|---|---|
| 1.1 | A: `/workbench` (also `/wb`, `/craft`), craft planks | Result appears and can be taken |
| 1.2 | A: put items in the grid, close the menu | Items return to the inventory, none lost |
| 1.3 | A: `/workbench`, walk far away with the menu open | Menu stays open |
| 1.4 | A: `/anvil`, rename and combine items | Works, XP is charged; items left inside return on close |
| 1.5 | A: `/grindstone`, disenchant | Works; XP drops; items return on close |
| 1.6 | A: `/stonecutter`, `/loom`, `/cartographytable`, `/smithingtable` | Each makes its result; items return on close |
| 1.7 | A: stand on a real anvil block, `/anvil`, use it many times | No crash; the block under A may wear down like a normal anvil |

## Inventory

| # | Do | Expect |
|---|---|---|
| 2.1 | A: `/enderchest` (`/ec`) | Own ender chest; contents match the block |
| 2.2 | A: `/ec B`, move an item | B's ender chest changes; title shows B's name |
| 2.3 | A: `/invsee B` | B's items live: row 1-3 inventory, row 4 hotbar, then armor and offhand |
| 2.4 | A: take an item out, put one in | B's inventory changes immediately |
| 2.5 | A: try to put an item in the last slots after offhand | Refused; the item stays with A |
| 2.6 | Give A `invsee` but not `invsee.modify` (permission mod) | Read-only: clicks do nothing |
| 2.7 | B logs out while A views | Menu closes |
| 2.8 | A: hold a block, `/hat`; `/hat remove` | Block on the head; back in the inventory |
| 2.9 | A: damage a tool, `/repair`; damage several, `/repair all` (`/fix`) | Fixed; `Repaired N item(s).` |
| 2.10 | A: hold dirt, `/repair` | "This item cannot be repaired." |
| 2.11 | A: hold 1 dirt, `/more`, `/more 10` | 64, then 10 |
| 2.12 | B: `/disposal` (`/trash`), put items in, close | Items are destroyed |

## /clearinventory (`/ci`, `/clean`)

| # | Do | Expect |
|---|---|---|
| 3.1 | A: `/ci` | Main inventory cleared; armor and offhand stay |
| 3.2 | A: `/ci **` | Armor and offhand cleared too |
| 3.3 | A: `/ci @s minecraft:dirt 5` | Only 5 dirt removed |
| 3.4 | A: `/ci B`, `/ci * minecraft:dirt` | B's items; dirt from everyone |
| 3.5 | A: `/clearinventoryconfirmtoggle`, `/ci`, `/ci` | First asks, second clears; a different command or 30 s resets it |
