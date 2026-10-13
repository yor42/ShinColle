# Issue #1 (ATTR-03): getAndRemoveItem Duplicates and Deletes Items

- Date: 2026-10-13
- Phase: 3
- Author: Claude Code
- Related issue/ADR: GitHub #1; gap ATTR-03 (`../gaps/2026-10-13-attributes-progression.md`)

## Scope
`InventoryHelper.getAndRemoveItem` is the item-take routine used by the crafting task (`TaskHelper.java:209`) and by ship-morph supplies (`MetamorphHelper.decrSupplies`, `MetamorphHelper.java:713`). The work was test-first: characterization tests, then the fix.

## Evidence
1. Red run: `./gradlew test` → BUILD FAILED. `InventoryHelperTest` tests=6, failures=6 (log `logs/2026-10-13-issue1-red.log`):

   | Case | Expected | Got |
   |---|---|---|
   | Take 3 of 10 | 3 | 10 |
   | Take 1 of 10 | 1 | 10 |
   | Take all 10 | 10 | 0 |
   | Take 5 when only 2 exist | 2 | 0 |
   | Take 4 across stacks of 2 and 5 | 4 | 5 |
   | Take 3 of 10 from a `TileEntityChest` | 3 | 10 |

2. Green run: `./gradlew build` → BUILD SUCCESSFUL (compileJava, jar, remapJar, test). `InventoryHelperTest` tests=6, failures=0; `TestEnvironmentTest` tests=2, failures=0 (log `logs/2026-10-13-issue1-green.log`).
3. Historical code (8bca563) copied the stack, then set the copy's size to the amount taken. In 1.10 a 0-size stack could still be copied.

## Confirmed findings
| Feature | Status | Confidence | Historical | Current 1.12.2 (before fix) | Desired | Evidence |
|---|---|---|---|---|---|---|
| Item take from inventory or chest | CONFIRMED_BUG → fixed (unit level) | HIGH | Returns exactly the amount taken | Partial take returns the whole original count (duplication). Whole-stack take returns nothing (**deletion**), because 1.12 `ItemStack.copy()` of a stack shrunk to 0 gives `EMPTY` | Returns exactly the amount removed | 1, 2 |

The deletion half is new; issue #1 and ATTR-03 only described the duplication.

**Fix** (`InventoryHelper.java`, 3 branches): copy the stack and set the copy's count before shrinking the source. The adjacent-chest branch already used `setCount`, but it shrank first, so it also lost whole-stack takes. It got the same reorder. No other change.

## Assumptions
- The adjacent double-chest branch is fixed by the same reorder but is not covered by a test, because `TileEntityHelper.getAdjChest` needs a world.

## Unknowns
- The manual in-game check (a ship's crafting task pulling from a chest) is not done yet. It needs a client session.

## Risks
- **Interaction with #4.** `decrSupplies` returns `itemGet.isEmpty()` (the inverted check, NET-04). Before this fix, taking the last single item returned `EMPTY`, so the inverted check accidentally reported "not found" after consuming the item. Now it always returns the taken item. Until #4 lands, ship-morph supply use behaves as described in #4 for every stack size. #4 is next in the batch.
- Players who relied on duplication lose it. That is intended.

## Recommended next actions
1. Manual check in a client session (see the task-0 report): a ship crafting task with a chest holding stacks larger than 1 and stacks of exactly 1. Item counts in chest + ship + output must add up.
2. Optionally comment on #1 that whole-stack takes also deleted items (a public action; needs approval).
3. Continue with #2 (NET-02).
