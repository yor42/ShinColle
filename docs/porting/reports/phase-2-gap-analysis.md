# Phase 2 Gate Report: Gap Analysis

- Date: 2026-10-13
- Phase: 2
- Author: Claude Code
- Related issue/ADR: ADR-0001; backlog `../gaps/2026-10-13-gap-backlog.md`

## Scope
Static gap analysis of eight subsystems against the historical baseline `8bca563` (upstream PinkaLulan 1.10.2):
- entity inheritance and lifecycle
- attributes and progression
- combat and equipment
- AI and pathfinding
- inventory and persistence
- rendering and animation
- network synchronization
- mod integrations

Not done:
- No game was launched, no test was written, and no implementation code was changed.
- Shipyard tile internals, player skills (`ShipSkillHandler`), per-class attribute overrides, and the 1.7.10 behavior were only partly covered or not covered.

**Phase 1 gap.** The Phase 1 deliverables were incomplete when this phase started on the user's instruction:
- No feature inventory, historical behavior records, test strategy or Phase 1 gate report existed. Only the build migration and the runClient smoke test were done.
- The historical research needed here was done inside Phase 2 (`../baseline/2026-10-13-historical-lineage.md`). The per-subsystem reports double as the feature inventory.
- A test strategy is sketched under "Phase 3 entry proposal" below.

## Evidence
1. Subsystem reports in `../gaps/`: `2026-10-13-{entity-lifecycle, attributes-progression, combat-equipment, ai-pathfinding, inventory-persistence, rendering-animation, network-sync, mod-integrations}.md`. Each was produced by a read-only `legacy-code-analyst` subagent and compiled by the main session.
2. Lineage: `../baseline/2026-10-13-historical-lineage.md`. The baseline `8bca563` was found in local git history and builds against MC `1.10.2-12.18.3.2221`.
3. Main-session spot verification by re-reading the cited source: ATTR-03, ATTR-08, NET-02, NET-04, ENT-02, ENT-08, CMB-01, CMB-02, AI-01, AI-03, INV-03, and the baseline ancestry. All were confirmed as described.
4. One subagent claim was rejected after checking: "8bca563 sits after the 1.12.2 init commit". It is not a descendant of `0aad501` and it builds 1.10.2.

## Confirmed findings
- 87 records across 8 subsystems (12 ENT, 12 ATTR, 9 CMB, 8 AI, 11 INV, 11 RND, 12 NET, 12 MOD), each with a proposed status, confidence, evidence and reproduction.
- **Overall shape:** most gameplay logic is unchanged from `8bca563` apart from API renames. The defects split into three groups:
  1. A small number of **port and fork regressions** with clear intended behavior (P0).
  2. **Inherited upstream bugs** (multiplayer trust, combat-roll and day/night tables) that need decisions.
  3. **Fork-added features** that need PLANNED_DEVIATION approval.
- **Save format:** no ship, player, Ship Spawn Egg or world-data NBT key was renamed or removed. New keys exist only on the Crane (FE/EU), and one config key was added.
- Status counts are in the backlog. No feature is CONFIRMED_WORKING, because there is no runtime or test evidence.

Top findings (all verified, details in the backlog):

| ID | Status | Conf. | Historical | Current | Desired (proposal) |
|---|---|---|---|---|---|
| ATTR-03 | CONFIRMED_BUG | HIGH | `stackSize = minsize` | returns the full stack: duplication | `setCount(minsize)` |
| AI-01 | CONFIRMED_BUG | HIGH | symmetric `expand` | the box cancels to the ship hitbox | symmetric `grow` |
| NET-04 | CONFIRMED_BUG | HIGH | `itemGet != null` = consumed | `isEmpty()` = consumed (inverted) | `!isEmpty()` |
| ENT-08 | CONFIRMED_BUG | HIGH | 64-block linear | 8-block (squared vs 64) | 64 blocks |
| AI-03 | CONFIRMED_BUG | HIGH | depth from `1 - frac` | +1 and 8-tick stale | baseline values |
| NET-02 | CONFIRMED_BUG | HIGH | missing (inherited) | missing | server OP check |

## Assumptions
- A static defect with an unambiguous intended behavior counts as CONFIRMED_BUG. Each still needs a reproduction first in Phase 3.
- Severity assumes default config and single-player or small trusted servers unless noted.

## Unknowns
- **Whether the game reaches a world under Cleanroom/Java 25.** The smoke test stopped at Post-Init. This gates every runtime check.
- Optional mods at runtime: Metamorph, Baubles, IC2 and TiC were never loaded.
- Old-save compatibility: no 1.10.2 or older 1.12.2 save fixture exists.
- Intent questions listed below.

## Risks
- **Persistent data:** fixes touching NBT load (INV-01, INV-03) must keep the key layout. Format changes need approval.
- **Gameplay balance:** fixing inherited bugs (CMB-01, CMB-02, ATTR-06) changes long-standing behavior and needs explicit decisions.
- **Wire protocol:** multiplayer-trust fixes change packets. Client and server must match; saves are unaffected.
- **Build/dependency:** runtime-testing Metamorph or Baubles requires adding them to the dev runtime (a scope change, which needs approval).

## Decisions needed from the maintainer
`/grill-with-docs` is not in this session's skill list; the installed equivalent is `mattpocock-skills:grilling`. Please work through these with it. Answers become PLANNED_DEVIATION entries, ADRs, or issue priorities.

1. **Combat balance (CMB-01, CMB-02):** fix triple hit and the day/night swap to the documented intent, or keep the as-played behavior as PLANNED_DEVIATION?
2. **Metamorph scope (MOD-01, MOD-03, NET-04, ATTR-06, ATTR-10, RND-07):** is ship-morph a supported feature on the legacy track? On the Modern track? This sets the NET-04 priority and whether Metamorph joins the dev runtime.
3. **Multiplayer trust model (NET-01/02/03/06/07, INV-04):** are public servers in scope? If yes, NET-02 moves into the first three tasks.
4. **Fork additions as PLANNED_DEVIATION?**
   - ATTR-05: Training Book not consumed at the cap.
   - ATTR-04: bowls no longer returned.
   - CMB-04: enchantments on aircraft.
   - CMB-07: 18.11-inch cannon stats.
   - MOD-04/05/07/08/09: IC2, FE, Mekanism, TiC, TOP.
   - RND-05: TESR→JSON migration.
   - RND-06 and AI-07: waypoint render instead of particle hints.
   - AI-08: auto emote defaults.
   - INV-02: Crane config resize.
5. **Old worlds (ENT-12, INV-01):** are worlds from pre-1.12 builds or pre-FE builds a supported migration target?
6. **Sound data (RND-01/02/03):** restore the dropped `pan`/`burnlove` entries and fix the typo, or change the config defaults?
7. **Smaller intent questions:**
   - AI-02: guard cadence.
   - NET-09: polling sync vs tracking-start sync.
   - ENT-07: revive at the player or at the cached position.
   - ENT-11: ships and portals.
   - ENT-04: does the boss cooldown require the ring when `Check_Ring=false`?
   - INV-08: state loss on death.
   - INV-10: keep `CapaTeitoku`?
   - MOD-12: dev-only `modImplementation` scopes.
8. **Phase 3 exit criteria (required by `../README.md`):** proposed below.

## Phase 3 entry proposal (for approval)
- **Task 0:** a runtime smoke run into a world, with a manual checklist: spawn, GUI, feed, combat, save and reload. Plus `src/test` bootstrap with one JUnit test (adding a test source set uses the existing JUnit 5 wiring; no dependency change expected).
- **First three tasks:** ATTR-03, AI-01 (+AI-02 if decided), NET-04/ATTR-01/02. Each is test-first, a minimal fix, and gets a regression-verifier report.
- **Proposed exit criteria:**
  - All P0 items fixed, or explicitly deferred with a reason.
  - Each fix has an automated test or a recorded manual check.
  - `./gradlew build` passes from an actual run.
  - A world-entry smoke run is recorded.
  - Every P2 and P3 item has a maintainer decision.
  - No NBT key or registry ID changed.
- **Test strategy sketch:**
  - Pure-logic JUnit tests for the extracted calculations: combat rolls, attribute tables, EXP and level cap, depth, search boxes, packet codecs, NBT DTO round trips.
  - Resource-consistency tests: `sounds.json`, models, lowercase literals.
  - Manual in-world checklists for entity behavior.
  - The pure-logic tests double as the first shared-core candidates for Phase 4.

## Files created or modified
Created:
- `docs/porting/gaps/2026-10-13-*.md` (8 subsystem reports and the backlog)
- `docs/porting/baseline/2026-10-13-historical-lineage.md`
- this report

Modified:
- `docs/porting/README.md` (phase line)
- `docs/porting/decisions/README.md` (pending decisions)
- `CONTEXT.md` (proposed terms)

No Java, resources, build files or assets were touched. Nothing was committed.

## Recommended next actions
1. Maintainer reviews the backlog and answers the questions above. **Stop here: architectural review before Phase 3.**
2. On approval, create the proposed GitHub issues (listed in the backlog) and start Phase 3 task 0.
