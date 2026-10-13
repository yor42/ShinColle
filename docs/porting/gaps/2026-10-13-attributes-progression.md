# Gap Analysis: Ship Attributes and Progression (ATTR-)

- Date: 2026-10-13
- Phase: 2
- Author: Claude Code (legacy-code-analyst subagent; ATTR-03 and ATTR-08 spot-verified by the main session)
- Related issue/ADR: backlog `2026-10-13-gap-backlog.md`

## Scope
Stats (Attrs, AttrsAdv, BuffHelper, Values, ConfigHandler scales and limits), EXP and levels, Training Book, Modernization, marriage, morale, fuel/grudge/ammo, potion buffs on ships, and the Metamorph supply paths. Not examined: `playerskill/ShipSkillHandler.java` in detail, the per-class `calcShipAttributesAdd*` overrides (about 30 classes), and Baubles `ringAbility` effects. Static analysis and diff against `8bca563`; nothing was run.

Most of the code in scope is unchanged from 8bca563 apart from 1.12 API renames. Most real defects are null-to-`ItemStack.EMPTY` and `stackSize`-to-`grow` translation slips from the port commit `7b71860`.

## Evidence
1. `intermod/metamorph/MetamorphHelper.java:713-716`: `return itemGet.isEmpty();`. 8bca563 `intermod/MetamorphHelper.java:809-813`: `return itemGet != null` semantics. 7b71860 introduced the inversion; cb6fbe4 simplified it.
2. **Main-session verification (2026-10-13):**
   - `utility/InventoryHelper.java:596-601` and `645-649`: `getTemp.shrink(minsize); getItem = getTemp.copy(); getItem.grow(minsize);`. The adjacent-chest branch at `:558-560` uses `setCount(minsize)`.
   - 8bca563 `InventoryHelper.java:621,682,753`: `getItem.stackSize = minsize;`.
   - Caller `TaskHelper.java:209` requests 1 item from a chest.
3. Main-session verification: `TaskHelper.java:858`: `luck += ship.getLevel() / ConfigHandler.maxLevel * 1.5F;` uses int/int division.
4. 57b124b changed the Training Book guard from `getLevel() < 150` (8bca563 `BasicEntityShip.java:1765-1787`) to `level < getLVCap()` (`BasicEntityShip.java:1584-1603`; `getLVCap` at `:1524`).
5. d8d43eb (iborvs, 2020-07-11, "Fix: Kanmusu not consuming items") rewrote feed consumption to `shrink(1)` plus clearing the slot (`InteractHelper.java:486-503`). Container-item return is commented out (`:489-497`).

## Confirmed findings

| ID | Feature | Status | Conf. | Historical (8bca563) | Current (HEAD) | Desired (proposal) | Evidence |
|---|---|---|---|---|---|---|---|
| ATTR-01 | Morph stats-screen supply buttons. **Same defect as NET-04; tracked there.** | CONFIRMED_BUG (port regression) | HIGH | Correct polarity | Inverted: free grant with no item. With one item, the item is lost, then the container branch "succeeds" and grants 9x base. | `return !itemGet.isEmpty()` | Ev. 1 |
| ATTR-02 | Repair Goddess auto-revive while morphed | CONFIRMED_BUG (same root cause as ATTR-01) | HIGH | Correct polarity | `MetamorphHelper.java:256-266`: with no Goddess, every lethal hit fully heals. With a Goddess, it is consumed and the player still dies. | Fixed by ATTR-01 | Ev. 1 |
| ATTR-03 | `getAndRemoveItem` returns the wrong count: **item duplication** | CONFIRMED_BUG (port regression) | HIGH | `stackSize = minsize` (correct) | Two of three branches return the full original count while removing only `minsize`. A crafting task pulling 1 from a 64-stack yields 64 in the ship and 63 in the chest. | `setCount(minsize)` in all branches | Ev. 2 (verified) |
| ATTR-04 | Bugs.txt "grudge, ammo, abyssal foods not consumed" | Partly stale: the GUI-button part is ATTR-01; right-click feed shows no static defect (no status for the feed path) | MEDIUM | `--stackSize` with `getContainerItem` (broken in 1.12, fixed by d8d43eb) | `InteractHelper.java:330-526`; ship self-consumption `BasicEntityShip.java:2704-2755`. `useCombatRation` shrinks even if `interactFeed` fails (safe only because of a pre-check). Container items (bowls) are no longer returned: candidate PLANNED_DEVIATION (d8d43eb). | Keep; characterize each food type | Ev. 5 |
| ATTR-05 | Training Book at the level cap (57b124b) | Candidate PLANNED_DEVIATION (commit states intent); no defect found | MEDIUM | Unmarried ship at 100 re-set to 100: book consumed, free full heal | Not consumed at the cap, with no feedback. The cap is duplicated in `addShipExp` (`:1166`), `ConfigHandler.maxLevel=150` ("TODO not configurable", `:138`), and the GUIs (`GuiShipInventory.java:1135`, `GuiMorphInventory.java:806`). | Keep; a single level-cap source | Ev. 4 |
| ATTR-06 | Training Book while morphed | UNKNOWN_INTENT | MEDIUM | Same (cap 150, ignores marriage) | `item/TrainingBook.java:68-91`: an unmarried morph ship at 100 levels to 150. 57b124b was not applied here, and this bypasses the 100 cap in `addShipExp`. | Use `getLVCap()` and do not consume at the cap | static |
| ATTR-07 | `ExpNext` staleness | CONFIRMED_BUG (inherited) | MEDIUM | Identical | `setExpNext()` (`:1160-1162`, `level*expMod+expMod`) runs only on NBT load and in client GUIs. `setShipLevel` (book) never refreshes it. The initial `ExpNext=40` (`:167`) is hard-coded regardless of `expMod`. | Refresh in `setShipLevel` and at construction | static |
| ATTR-08 | Fishing luck level bonus | CONFIRMED_BUG (inherited) | HIGH | Same (`TaskHelper.java:937`) | Integer division: 0 below level 150 | `(float) level / maxLevel * 1.5F` | Ev. 3 (verified) |
| ATTR-09 | Stat pipeline (`updateAttrsRaw`, `calcAttrsBuffed`, `checkAttrsLimit`, `ShipAttrMap`) | No status: diff shows no semantic change (not evidence of working) | MEDIUM | `utility/BuffHelper.java`, `Attrs*`, `Values` | `BuffHelper.java:71-112,750-808`. The `EquipAttrsMain` key change is equivalent (all SUBIDs are 0). The comment at `:87` says DEF 0.133, the code uses 0.00133 (stale doc). | Characterization tests first | diff |
| ATTR-10 | Grudge hunger multiplier, ship vs morph | UNKNOWN_INTENT | LOW | Same in both | Ship: `value *= 1 + 2*lv` (`BasicEntityShip.java:2634-2687`). Morph: `1 + lv` (`MetamorphHelper.java:560-575`). The tooltip says +200%/lv. | Ask the maintainer | static |
| ATTR-11 | Modernization Toolkit and marriage | No status: no defect found | MEDIUM | Same logic | `InteractHelper.java:44-67,132-174`. The kit is not consumed when all slots are capped. Marriage sets morale 16000, +3 bonus, cap 150. | none | diff |
| ATTR-12 | Morale tiers, fuel/ammo consumption, potion buffs | No status: no regression | MEDIUM | `BasicEntityShip.java` updateMorale at 3623+ | `ID.java:547-559`; `BuffHelper.java:668-690`; `BasicEntityShip.java:2521-2750,3195-3219`. Inherited stubs: `BuffHelper.java:553` TODO; `buffSaturation` is not configurable. | none | diff |

### Per-record details

| ID | Reproduction | Severity | Depends on | Stabilize before Modern? | Candidate regression tests |
|---|---|---|---|---|---|
| ATTR-01 | See NET-04 | HIGH | Metamorph | yes | Consume-decision unit test |
| ATTR-02 | Morph with no Repair Goddess; take a lethal hit (expected: death) | HIGH | ATTR-01 | yes | Goddess consumed once per save |
| ATTR-03 | Chest paired to a ship crafting task holds 64 planks; run one craft cycle; compare chest count and ship slot | HIGH (duplication) | TaskHelper crafting task | yes | Stack-arithmetic unit test on an extracted `takeFrom(stack, n)` |
| ATTR-04 | Feed each item type (Grudge, Ammo, abyssal foods, vanilla food, potion) from a stack of 1 in survival | LOW | none | no | Manual checklist |
| ATTR-05 | Unmarried ship at 100 + book: nothing happens, book kept. Married: +5..10 up to 150. | LOW | marriage | no (extract the cap rule) | `lvAfterBook(level, married, roll)`; no consumption at the cap |
| ATTR-06 | Morph unmarried at 100; hold the book 80 ticks; level > 100? | MEDIUM | Metamorph, ATTR-05 | yes (same cap constant) | Same as ATTR-05 |
| ATTR-07 | Book a ship 1→8, grant EXP, read the server `ExpNext` | LOW | config `expMod` | no | `expToNext` table; `addExp` simulation (carry, cap, one level per call) |
| ATTR-08 | Log `luck` for a level-80 fishing ship (expected about 0.8, current 0) | LOW | none | no | Float table |
| ATTR-09 | n/a | n/a | equipment (CMB) | no, but characterize | Golden tables for `updateAttrsRaw` per class at levels 1/50/100/150 with bonus 0..3; `calcAttrsBuffed`; `checkAttrsLimit` |
| ATTR-10 | Compare grudge drain under hunger, ship vs morph | LOW | Metamorph | no | `consumedGrudge(value, hunger, isMorph)` |
| ATTR-11 | Kit at the limit is not consumed; marry, then +3 bonus and cap 150 | LOW | none | no | Seeded `addAttrsBonusRandom` |
| ATTR-12 | n/a | n/a | none | no | Morale-tier function; grudge arithmetic |

## Assumptions
- `player` is always non-null in the Training Book interact path, so the dropped null check is harmless.
- "No semantic change" verdicts come from whitespace-normalized diffs, not from execution.

## Unknowns
- `ShipSkillHandler` was rewritten by the fork (+287/-189 lines); morph skill costs, cooldowns and EXP are not verified.
- The per-class `calcShipAttributesAdd*` overrides.
- Baubles ring ability effects.
- Hostile ship scaling values compared with `cfg-info.md`.
- Kills grant no EXP in either version (`EventHandler.java:204-210`); whether that is intended is undocumented.

## Risks
- ATTR-03 duplicates items in survival through the crafting task. It is an economy exploit and a likely source of player reports.
- Fixing ATTR-07 changes progression pacing slightly for existing saves (no format change).

## Minecraft-independent logic candidates
`BuffHelper.updateAttrsRaw` and `calcAttrsBuffed`; `Attrs.checkAttrsLimit` and `addAttrsBonusRandom`; `AttrsAdv` reset arrays; the numeric maps in `Values` (`ShipAttrMap`, `HostileShipAttrMap`, `MoraleAttrs`) and `ID.Morale`; the EXP curve and level-cap rule; morale update and tier selection; grudge and ammo consumption arithmetic. Each needs a seam for its static `ConfigHandler` reads.

## Recommended next actions
1. Stabilization candidates: ATTR-03 (duplication), ATTR-01/02 (with NET-04).
2. Characterization golden tables for ATTR-09 before any extraction.
3. Maintainer questions: ATTR-06, ATTR-10, and whether ATTR-05 and the ATTR-04 bowl change are PLANNED_DEVIATION.

## Phase 3 updates
- 2026-10-13 ATTR-03 (#1): reproduced by unit tests and fixed. Whole-stack takes also **deleted** items (1.12 `copy()` of a 0-count stack is `EMPTY`), which the Phase 2 record missed. A manual in-game check is pending. See `../reports/2026-10-13-issue-1-item-duplication.md`.
