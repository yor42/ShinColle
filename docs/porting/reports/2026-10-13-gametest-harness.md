# In-Game Test Harness (Feasibility Spike)

- Date: 2026-10-13
- Phase: 3
- Author: Claude Code
- Related issue/ADR: task 0 client half (`2026-10-13-phase3-task0.md`); #1 (crafting-task check scripted in run 4; manual gaps remain); P-13

## Scope
Can the client half of the in-game checks run without a person at the keyboard? This spike built a dev-only harness and ran it against the current code. As of run 4, the #1 crafting-task check (chest take and craft through the ship's task logic) is scripted; its remaining manual gaps are listed under Run 4. It does not cover multiplayer, Metamorph or Baubles.

## How it works
- `src/gametest/` is a separate source set. It holds a second mod, `shincolle_gametest`, which is client-only and does nothing unless `-Dshincolle.gametest=true` is set. It is never in the mod jar.
- `./gradlew runClient -Pgametest` puts it on the dev mod path and sets the property (`build.gradle`). Without `-Pgametest`, nothing changes.
- From the main menu, the harness:
  - creates a fresh superflat creative world;
  - steps through a script on client ticks;
  - acts through the real client controller (`PlayerControllerMP`, the same packets as mouse clicks);
  - checks state on the integrated server thread (`callFromMainThread`);
  - saves screenshots;
  - writes `run/client/gametest/results.txt`, logs every line as `[GAMETEST]`, then quits.
- Each step has a tick timeout. An exception or timeout fails that step and the run goes on.
- An integrated server with a real player also avoids the dedicated-server problem, where entities stop ticking with no player online (task-0 Unknowns).
- Cleanroom detail: a dev folder only loads as a mod when it has a matching resources folder (`classes/java/X` with `resources/X`). That is why the harness ships an `mcmod.info`. Run 1 had none and never started (evidence 1).

## Evidence
All runs used JDK 25.0.4 and `./gradlew runClient -Pgametest` on Windows 11 with a visible window, default dev runtime (Mekanism, Mantle, TOP; no Metamorph or Baubles).

1. Run 1: 17 mods loaded; `debug.log` shows `build/classes/java/gametest` "collecting as developing mod" but only `main` was added. No `[GAMETEST]` lines; the client idled at the main menu until the 900 s timeout. Fixed by adding `src/gametest/resources/mcmod.info`.
2. Run 2: 18 mods; 5 passed, 1 failed (combat). The ship had 0 ammo and "Can Melee" off (see the GUI screenshot), so it could not attack. This was a setup error in the harness.
3. Run 3 (`logs/2026-10-13-gametest-run3-results.txt`, `logs/2026-10-13-gametest-run3.log`): 6 passed, 1 failed, 1 skipped. Wall time from world creation to quit was about 50 s. `latest.log` has no `Exception` lines.

   | Step | Result | Detail |
   |---|---|---|
   | enter-world | PASS | new world, 18 mods |
   | spawn-ship-with-egg | PASS | egg meta 2 → `EntityDestroyerI`, level 1, HP 20.3/20.3, owner UID 100 and UUID match the player, `CanDrop` true at tick 30 |
   | open-ship-gui | PASS | sneak + right-click opens `GuiShipInventory`; the screenshot shows the panel and ship model (`logs/2026-10-13-gametest-ship-gui.png`) |
   | feed-ship | PASS | cooked beef: food 0 → 1 (max 10); creative, so the beef was not used up |
   | feed-ammo | PASS | `Ammo` meta 0: light ammo 0 → 15 |
   | ship-attacks-husk | FAIL | husk 4.8 blocks away, not damaged in 790 ticks; ship target stayed null the whole time; not sitting; PassiveAI off; 15 light ammo |
   | save-quit-reload | PASS | same UUID found after reload with level 1, owner UID 100, HP 20.3/20.3 |
   | crafting-task-take | SKIP | not scripted yet in run 3; scripted in run 4 (see Run 4) |

4. Combat screenshot (`logs/2026-10-13-gametest-combat.png`): the ship and the husk both render; the ship is idle.
5. Run 4 (`logs/2026-10-13-gametest-run4-results.txt`, `logs/2026-10-13-gametest-run4.log`): see the Run 4 section below.

## Run 4
- Setup: JDK 25.0.4, `./gradlew.bat runClient -Pgametest`, run twice.
  - Run 4a had a probe bug: it read `taskEntries` instead of `executingTaskEntries`. Its results are not saved; only its light-ammo value (18) is recorded here.
  - Run 4b is the saved evidence: `logs/2026-10-13-gametest-run4-results.txt`, `logs/2026-10-13-gametest-run4.log` (0 `Exception` matches; 13 PASS/FAIL lines), and `logs/2026-10-13-gametest-run4-husk-baseline.png`.
- Result (run 4b): 10 PASS, 3 FAIL. The 3 FAILs are the intended diagnostic results of the combat variations (see Probe findings), not harness errors.

| Step | Result | Detail (run 4b unless noted) |
|---|---|---|
| enter-world | PASS | world `gametest-1791889629086`, 18 mods |
| spawn-ship-with-egg | PASS | `EntityDestroyerI` level 1, HP 20.3/20.3, owner UID 100, UUID matches the player, `CanDrop` true, tick 29 |
| open-ship-gui | PASS | `GuiShipInventory` |
| feed-ship | PASS | food 0 → 1 (max 10), beef left 8 |
| feed-ammo | PASS | light ammo 0 → 16 (run 4a: 18, so the amount varies between runs) |
| feed-grudge | PASS | grudge 110 → 105 (the step's pass criterion is not restated here) |
| ship-attacks-husk | FAIL | husk at 6 blocks; target never set in 392 ticks |
| ship-attacks-husk-after-grudge | FAIL | same setup after feed-grudge; husk undamaged after 392 ticks, target never set |
| ship-attacks-husk-close-no-teleport | FAIL | husk at 3 blocks, player not moved; husk undamaged after 392 ticks, target never set |
| ship-attacks-husk-inside-range | PASS | husk at 1.2 blocks; target set within 16 ticks; husk HP 17.80/20.0 after 48 ticks |
| crafting-task-stack-gt1 | PASS | planks 5-stack in chest; ended as chest 0 + ship 1, sticks 8; distractor cobblestone 8 unchanged |
| crafting-task-stack-eq1 | PASS | 3 single-plank stacks; ended as ship 1, sticks 4; cobblestone unchanged |
| save-quit-reload | PASS | same UUID after reload, level 1, owner UID 100, HP 18.75/20.3 (run 3 reload: 20.3/20.3; cause of the lower HP not investigated) |

### Crafting task (#1) method and limits
- Setup ran on the server thread with these shortcuts:
  - the waypoint block and chest were placed with `setBlockState`;
  - `setPlayerUID` and `setPairedChest` were used instead of the wrench;
  - the recipe paper (oak planks in grid slots 1 and 4 → sticks) was inserted directly into ship slot 22;
  - `Task=4` and `TaskSide` bit 18 were set directly (check metadata);
  - `setGuardedPos(wp, dim 0, type 1)`.
- `TaskHelper.onUpdateCrafting` then ran on the ship's own ticks. Counts cover the chest, ship slots 0-23 and ground item entities.
- Conservation held in both cases: `planks_total + 2*crafts == planks_before` (5 = 1 + 2×2; 3 = 1 + 2×1). No loss or duplication was seen.
- Gaps (not exercised):
  - no input-level GUI, wrench or recipe-paper driving;
  - single-chest branch only (the double-chest branch is untested);
  - ore-dictionary and NBT flags are not exercised;
  - the ground-drop output path was not hit.
- Status: the #1 crafting-task path is CONFIRMED_WORKING at MEDIUM confidence, for the scripted single-chest, planks-to-sticks case only. The manual gaps above remain open for #1.

## Confirmed findings
| Feature | Status | Confidence | Historical | Current 1.12.2 | Desired | Evidence |
|---|---|---|---|---|---|---|
| Client world entry with ShinColle (default runtime) | CONFIRMED_WORKING | HIGH | n/a | Works | n/a | 3, 5 |
| Ship Spawn Egg, client right-click path | CONFIRMED_WORKING | HIGH (one egg type) | n/a | Ship spawns owned, full HP, `CanDrop` set | n/a | 3, 5 |
| Ship GUI open (sneak + right-click) and rendering | CONFIRMED_WORKING | MEDIUM (opens and draws; buttons not exercised) | n/a | Works | n/a | 3, 5, screenshot |
| Feeding food and ammo items | CONFIRMED_WORKING | MEDIUM | n/a | Values rise | n/a | 3, 5 |
| Ship persistence across save, quit and reload in single player | CONFIRMED_WORKING | MEDIUM (level, owner, HP only) | n/a | Works | n/a | 3, 5 |
| Crafting task take and craft, single chest, planks to sticks (#1 path, scripted case) | CONFIRMED_WORKING | MEDIUM (one recipe, one chest, creative; see Run 4 gaps) | n/a | No loss or duplication in 2 scripted cases | n/a | 5 |
| Ship attacks a hostile mob inside its attack range | CONFIRMED_WORKING | MEDIUM (one run, one mob type, 1.2 blocks) | n/a | Target set, attack runs, damage applied | n/a | 5 (inside-range PASS, probe) |
| Automated client checks via the harness | CONFIRMED_WORKING | HIGH | n/a | about 50 s per run (run 3); run 4 wall time not recorded | n/a | 3, 5 |

The task-0 "Health 4.0" oddity is specific to `/summon`: the egg path spawns at full HP.

## Probe findings (run 4b)
Facts from the run 4b probe unless marked as a hypothesis. No status is assigned to auto-targeting at 3 to 6 blocks; it is under investigation.

- Level-1 Destroyer I: `attrRange` = 2.008 and `aiRange` = 2. `EntityAIShipRangeTarget.updateRange` (lines 162-169) truncates with `(int)`; the `max(2, FollowMax+2)` minimum applies only when range < 2. `FollowMin/Max` = 3/12.
- The search box is `bb.grow(range, range*0.75, range)` (`EntityAIShipRangeTarget.java:81-109`). A husk at 3 or 6 blocks gives `inAABB=false`; at 1.2 blocks, `inAABB=true`.
- In all variations, from t=8 to t=392: `TargetHelper.Selector` (the real class) true; `canEntityBeSeen` true; not sitting; PassiveAI off; 16 light ammo; `EntityAIShipRangeTarget@5` and `EntityAIShipRevengeTarget@1` registered.
- Grudge and owner position made no difference in the tested variations.
- Inside range: `RangeTarget` sets `entityTarget`, `EntityAIShipRangeAttack@11` runs, and damage occurs. The combat chain works once a target exists.
- `NoFuel=true` at t=8 after spawn, with empty task lists; cleared by t=16. Read as a startup state; not verified in source.
- Hypothesis (not shown to be a bug): a level-1 Destroyer I only engages targets within about 2 blocks.
- Unknowns:
  - manual-play confirmation (the harness drives packets, not input);
  - AntiAir, AntiSS and PVPFirst were not varied.

### Historical check
Source: coordinator follow-up, supplied to the porting-doc-writer. The code citations were spot-checked by grep (`Values.java:32` DDI row, `BuffHelper.java:680` morale tier id 3 against `ID.Morale.L_Tired`); the arithmetic and the 8bca563 comparisons are as supplied.

- **Verdict:** inherited upstream behavior, not a 1.12.2 regression. Confidence HIGH for the code path, MEDIUM for gameplay equivalence.
- **Range rule:** `EntityAIShipRangeTarget.updateRange` is identical to 1.10.2 `8bca563` apart from formatting and `expand()` → `grow()` (`1d9f4a2`). The rule `(int) getAttackRange(); if (range < 2) range = max(2, FollowMax+2)` has been present since `d482e9c` (2016-12-21). An earlier test port, `7c48a1e` (2016-11-24), used a 16-block floor instead.
- **Where 2.008 comes from:**
  - DDI base HIT is 6F with a HIT type multiplier of 0.4F (`Values.java:32` at HEAD; `:56` at `8bca563`). At level 1 the raw value is 6 + (bonus+1)×1×0.02×0.4 = 6.008 (`BuffHelper.java:94`).
  - The morale tier "Exhausted" (id 3, morale ≤ `L_Tired` 900, which includes morale 0) applies HIT −4 (`Values.java:908-914`; `BuffHelper.java:676-682, 759`). The table is identical at `8bca563`.
  - 6.008 − 4 = 2.008. Correction (historical research): the constructor default is morale 60 (`BasicEntityShip.java:167-176`, index `ID.M.Morale`, `ID.java:527`), the same in `7c48a1e`, `d482e9c`, `3528989` and `8bca563`. 60 is inside the Exhausted tier (≤ 900), so the −4 applies from spawn. Morale 0 comes from the no-fuel zeroing (`updateFuelState(nofuel)`, `BasicEntityShip.java:2768-2770`; `8bca563` `:3121`), which explains NoFuel=true at t=8 in run 4b.
- **Assumption:** the probe logged `attrRange` only, not the morale tier, so the tier is inferred from the arithmetic, not observed.
- **Consequence for the harness:** the `ship-attacks-husk` baseline FAIL reflects a test setup choice (the Exhausted tier from spawn, or from no-fuel zeroing, plus a husk beyond 2 blocks), not a regression. The step should set a normal morale (above 2100) or place the husk within 2 blocks.
- **Status for "ship auto-targets hostiles at 3-6 blocks":** not a regression; upstream behavior in the Exhausted morale tier (spawn default 60, or 0 after no-fuel zeroing). No feature-status value is assigned here; the classification is recorded in `docs/porting/gaps/2026-10-13-ai-pathfinding.md` (AI-10).
- **Unknowns:**
  - whether the spawn default of morale 60 (Exhausted tier) is intended (a human design decision; the human's "morale 0 at spawn" premise was corrected by historical research);
  - 1.7.10 behavior (not checked).

## Assumptions
- One egg type (Destroyer I, meta 2) stands for normal ships. Other classes are not covered.
- Creative mode does not change the paths under test, except item consumption, which is not asserted.
- The inside-range PASS is one run with one mob type. It does not establish range behavior in general.

## Unknowns
- Light ammo amount from `Ammo` meta 0: observed +15 (run 3), +16 (run 4b), +18 (run 4a, not saved). `InteractHelper.java:387-389` suggests 30-39. The values vary between runs, which is consistent with randomization, but this is not confirmed in source. Not investigated further.
- Ship HP after reload in run 4b (18.75/20.3) is lower than in run 3 (20.3/20.3). Cause unknown; the probe does not record damage taken during the run.
- The MOD-01 client attack-particle path is still unverified. Run 4 had attacks, but its results contain no particle check.

## Risks
- The harness drives packets directly, so it skips mouse picking and key handling. A bug that only exists in input handling would not show.
- A visible window is required (LWJGL2). It cannot run headless or in CI without a virtual display.
- A run takes over the desktop window for about 1 to 2 minutes, including Gradle start-up.
- Changes `build.gradle` (new source set, opt-in property). No dependency or version change.
- `src/gametest/.../GameTestRunner.java` was changed for run 4 and is uncommitted (git status shows it modified). This report does not review that change.
- The probe reads AI internals by reflection. Run 4a showed that a wrong field name (`taskEntries` vs `executingTaskEntries`) silently produces a wrong reading, so probe output needs the field names checked against the code.

## Recommended next actions
1. Done (run 4): the #1 crafting-task check is scripted for a single chest and a planks-to-sticks recipe. Still manual for #1: the double-chest branch, ore-dictionary and NBT flags, input-level GUI, wrench and recipe-paper driving, and the ground-drop output path.
2. Done (run 4): the no-target probe (see Probe findings). Done: the historical check of the attack range (upstream behavior, not a regression). No regression issue should be filed. Human decision (2026-10-13): no morale change is planned; the spawn default (60) and the no-fuel zeroing stay as they are. Recorded as AI-10 in `docs/porting/gaps/2026-10-13-ai-pathfinding.md` (UNKNOWN_INTENT, resolved as keep-as-is). The harness combat step sets morale explicitly (above 2100) rather than relying on the spawn default; this is an open harness change, not yet made.
2a. Issue #1 was closed on 2026-10-13, citing the run 4 evidence (scripted crafting-task check, Run 4 section above).
3. Adjust the harness combat steps to set a normal morale (or place the husk within 2 blocks), so the baseline reflects intended combat setup. Then add steps as later fixes land: #4 morph supplies (with `-Pdev_runtime_morph_baubles=true`), #7/INV-12 load paths, AI-01 guard mode.
4. Keep the harness dev-only. Commit only on request.

## Note: dedicated-server runs need fresh state (issue #9, 2026-10-13)
- Each dedicated run should start with `run/server-gametest` and its `world-gametest` level deleted (`build.gradle:192` sets the server directory; the level name is `world-gametest`, per `logs/2026-10-13-gametest-dedicated-final-server.log:227`).
- Reported by the parent (not reproduced in a saved log): a ship from a previous run persisted in the world and broke the `expected exactly 1 ship` precondition. The saved failing run `logs/2026-10-13-gametest-dedicated-results.txt` shows `ships=0` at the precondition and after the egg step, so that saved file does not show the persisted-ship cause. Unknown until the failing run's log is saved or a fresh rerun is recorded.
- The eight-entity pre-join workaround used in the issue #2 run (see `2026-10-13-issue-2-op-check.md`, Harness workarounds) was removed with the #9 fix. In `logs/2026-10-13-gametest-dedicated-issue9-results.txt:15` the player has entity id 0 and the run passes 8/8 with no padding entities. No padding code remains in `src/gametest` (grep, no matches).
- Single-player runs are not affected by this note.
- Fix status: the #9 fix and the #9b follow-up are verified (regression-verifier, final snapshot at HEAD `2403673` plus the working tree) and uncommitted. See `2026-10-13-issue-9-entity-id-zero.md`.

## Single-player harness flakiness (run 8 and the run 9 fixes, 2026-10-13)

### Symptom
- `logs/2026-10-13-gametest-run8-results.txt`: `SUMMARY passed=5 failed=9`. Failures include `feed-grudge` and the combat steps with `ship ... not loaded on server` (lines 61-64), and `save-quit-reload: ship ... not found after reload` (line 68).
- `logs/2026-10-13-gametest-run8-attempt1-results.txt`: `SUMMARY passed=5 failed=9` (`save-quit-reload` not found, line 68).
- `logs/2026-10-13-gametest-run8-attempt3-results.txt`: `SUMMARY passed=10 failed=4` (`save-quit-reload` not found, line 158).

### Root cause (as analysed by the coordinator from the run 8 logs; not re-verified by the porting-doc-writer)
- Combat miss rate: up to 50% (`BasicEntityShip.java:2228` and `:2282`; `CombatHelper.java:120-145`). Combined with a husk that melee-killed the ship inside the 400-tick engage limit. One example ran from HP 7 to 0 between t=280 and t=376.
- `refuel()` only topped up when grudge was below 100. One run started at grudge 152, ran out of fuel and reached morale 0.
- HP, ammo and level carried over from one step to the next.
- The fresh-dev-world hypothesis was refuted: each run creates a new world, `gametest-<timestamp>` (for example run 8, `enter-world`, line 2).

### Fix (harness only; `src/gametest/`)
- `refuel()` (`GameTestRunner.java:378`) now resets grudge to 1000, HP to max, `deathTime`, fire, `hurtResistantTime` and light ammo to 60, on every call (`:384-386`).
- All combat husks are set `setNoAI(true)` (`GameTestRunner.java:427`).
- The engage timeout went from 400 to 800 ticks (`GameTestRunner.java:309-313`), because the first hit took 56 to 392 ticks.
- `save-quit-reload` compares the level against the level from before quitting, not against a fixed level 1.

### Results
- `logs/2026-10-13-gametest-run9a-results.txt`, `run9b-results.txt`, `run9c-results.txt`: 14/14 each, with the #9 edits in (`SUMMARY passed=14 failed=0`). `run9b` `save-quit-reload` reads `level=2` (the pre-quit level).
- `logs/2026-10-13-gametest-run9d-stashed-network-results.txt`: network files stashed. `SUMMARY passed=13 failed=1`; the only failure is `save-quit-reload: ship changed: expected level=1 ownerUID=100, got level=2` (line 192). This was before the level-comparison fix.
- `logs/2026-10-13-gametest-run9a-prelevelfix-results.txt`, `run9b-prelevelfix-results.txt`, `run9c-prelevelfix-results.txt`: 14/14 each, before the level fix.

### Limits
- Three green runs are evidence of stability in this setup, not proof.
- The husks no longer move or attack (`setNoAI(true)`), so melee survival is not measured by these steps.

### Observation (not changed)
- A ship fires only a few shots and misses up to 50%. This is the original combat tuning. It was not compared with 1.10.

### Run 7 count (reconciled)
- `logs/2026-10-13-gametest-run7-results.txt`: 14 `PASS` lines (`^PASS` count = 14), 0 `FAIL`, `SUMMARY passed=14 failed=0`. The list is in `2026-10-13-issue-9-entity-id-zero.md` (Evidence 10). No document in `docs/` gives a count of 15 for run 7 that could be located; the 15 may come from an unsaved message.

### Coverage lost by the harness fix
Recorded as known harness gaps. Neither is fixed by this section.
- **C1: no test of a ship reacting to a mobile hostile that attacks it.** Nothing now checks the revenge-target path or HP loss from a hostile that moves and attacks the ship. This coverage ran incidentally before the combat husks got `setNoAI(true)`; the fix removed it.
- **C2: `ship-attacks-husk-after-grudge` no longer proves that feeding grudge restores engagement.** `refuel` sets grudge to 1000 at the start of every combat step, so the grudge value at that step is not the value the feed produced. The separate `feed-grudge` step still asserts that NoFuel clears (`GameTestRunner.java:539-577`), but it does not show that engagement is restored.
