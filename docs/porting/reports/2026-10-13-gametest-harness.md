# In-Game Test Harness (Feasibility Spike)

- Date: 2026-10-13
- Phase: 3
- Author: Claude Code
- Related issue/ADR: task 0 client half (`2026-10-13-phase3-task0.md`); #1 manual check; P-13

## Scope
Can the client half of the in-game checks run without a person at the keyboard? This spike built a dev-only harness and ran it against the current code. It does not cover the #1 crafting-task check (still manual), multiplayer, or Metamorph or Baubles.

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
   | crafting-task-take | SKIP | not scripted yet |

4. Combat screenshot (`logs/2026-10-13-gametest-combat.png`): the ship and the husk both render; the ship is idle.

## Confirmed findings
| Feature | Status | Confidence | Historical | Current 1.12.2 | Desired | Evidence |
|---|---|---|---|---|---|---|
| Client world entry with ShinColle (default runtime) | CONFIRMED_WORKING | HIGH | n/a | Works | n/a | 3 |
| Ship Spawn Egg, client right-click path | CONFIRMED_WORKING | HIGH (one egg type) | n/a | Ship spawns owned, full HP, `CanDrop` set | n/a | 3 |
| Ship GUI open (sneak + right-click) and rendering | CONFIRMED_WORKING | MEDIUM (opens and draws; buttons not exercised) | n/a | Works | n/a | 3, screenshot |
| Feeding food and ammo items | CONFIRMED_WORKING | MEDIUM | n/a | Values rise | n/a | 3 |
| Ship persistence across save, quit and reload in single player | CONFIRMED_WORKING | MEDIUM (level, owner, HP only) | n/a | Works | n/a | 3 |
| Automated client checks via the harness | CONFIRMED_WORKING | HIGH | n/a | about 50 s per run | n/a | 3 |

The task-0 "Health 4.0" oddity is specific to `/summon`: the egg path spawns at full HP.

## Assumptions
- One egg type (Destroyer I, meta 2) stands for normal ships. Other classes are not covered.
- Creative mode does not change the paths under test, except item consumption, which is not asserted.

## Unknowns
- **Ship does not target a nearby husk (run 3).** This is not explained yet. `EntityAIShipRangeTarget` is registered after tick 10 (`BasicEntityShip.java:1865-1870`), and `TargetHelper.Selector` accepts `EntityMob`. Candidates:
  - the attack range at level 1 (`updateRange`, minimum `FollowMax + 2`);
  - `OnSightChase` with `canSee`;
  - the AI being preempted by follow-owner after the player teleport;
  - a real regression.

  This is not AI-01, which is guard mode only. It needs a probe that logs range and selector results, and a comparison with a hand-played session, before it is called a bug.
- Light ammo +15 from `Ammo` meta 0, where `InteractHelper.java:387-389` suggests 30-39. Something downstream may scale it. Not investigated.
- The MOD-01 client attack-particle path is still unverified, because no attack happened.

## Risks
- The harness drives packets directly, so it skips mouse picking and key handling. A bug that only exists in input handling would not show.
- A visible window is required (LWJGL2). It cannot run headless or in CI without a virtual display.
- A run takes over the desktop window for about 1 to 2 minutes, including Gradle start-up.
- Changes `build.gradle` (new source set, opt-in property). No dependency or version change.

## Recommended next actions
1. Script the #1 crafting-task check (recipe paper, guard on a chest, task side) so #1 can close without manual play.
2. Probe the no-target result (log range and selector per 8 ticks); then open an issue if it reproduces in manual play. Needs approval to file.
3. Add steps as later fixes land: #4 morph supplies (with `-Pdev_runtime_morph_baubles=true`), #7/INV-12 load paths, AI-01 guard mode.
4. Keep the harness dev-only. Commit only on request.
