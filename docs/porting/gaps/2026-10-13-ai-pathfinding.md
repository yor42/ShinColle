# Gap Analysis: AI Behavior and Pathfinding (AI-)

- Date: 2026-10-13
- Phase: 2
- Author: Claude Code (legacy-code-analyst subagent; AI-01 and AI-03 spot-verified by the main session)
- Related issue/ADR: backlog `2026-10-13-gap-backlog.md`

## Scope
`ai/**` (23 files) and its dependencies: `EntityHelper` (`checkDepth`, `updateShipNavigator`, `moveEntityWithHeading`, `updateWaypointMove`), `TargetHelper`, `TaskHelper`, `FormationHelper`, the `setAIList()` methods, waypoint code, and auto emote. Compared with `8bca563` using whitespace-, brace- and comment-normalized token diffs. Nothing was run. Not covered: the sddsd2332 time-announcement loop fix, and full task, formation and waypoint-chain semantics.

## Evidence
1. AI task registration is identical: 135 normalized `addTask(` lines in both revisions, same multiset (md5 equal). Per-file attribution was not compared.
2. Logic is unchanged apart from API renames in Sit, Flee, FollowOwner, AttackOnCollide, CarrierAttack, Wander, SkillAttack, RevengeTarget, RangeTarget, AircraftAttack, RangeAttack, OpenDoor and PickItem. `ShipPathFinder`, `ShipPathHeap`, `ShipPathPoint`, `ShipPath` and `ShipMoveHelper` are unchanged apart from formatting. `TaskHelper` and `TargetHelper` show only mechanical porting.
3. Main-session verification, `EntityAIShipGuarding.java:316-318`:
   ```
   fireRange = bbox.grow(r*0.9, r*0.6, r*0.9);
   fireRange = fireRange.grow(r*-0.9, r*-0.6, r*-0.9);
   ```
   The second line cancels the first. 8bca563 `:344` used a single `expand(...)`, which was symmetric in 1.10.
4. Main-session verification, `EntityHelper.java:138,147,156,168`: `liquidBlocks = 1`, then the loop starts at `y = py` (already known liquid) and increments again. Depth = `liquidBlocks - (posY - py)`. Recomputed only when `ticksExisted % 8 == 0` (`:120-122`). 8bca563 `:139` initialized depth = 1 and iterated from `i = 1`, every tick.

## Confirmed findings

| ID | Feature | Status | Conf. | Historical (8bca563) | Current (HEAD) | Desired (proposal) | Evidence |
|---|---|---|---|---|---|---|---|
| AI-01 | Guard-mode target search box collapses to the ship's own hitbox | CONFIRMED_BUG (fork regression: 7296046, misaka10843, 2022) | HIGH | Symmetric `expand(0.9r, 0.6r, 0.9r)` | `grow` then `grow(-…)` cancel out. Both guard firing paths (`:185-187`, `fireControl` `:500`) find only overlapping entities. | One symmetric box sized by range | Ev. 3 (verified) |
| AI-02 | Stationary guard fire cadence (`fireControl`) | CONFIRMED_BUG for the structure; intended cadence UNKNOWN_INTENT | MEDIUM | No `fireControl`: guard fire only while moving, with per-tick delay decrement | `shouldExecute` calls `fireControl` every 32 ticks (`:116-119`), which decrements the delay by 1 per call (`:492-534`), so an idle guard fires far slower than `maxDelayTime`. Debug strings are built unconditionally (`:321-324`). Added by 7296046. | Per-tick cadence matching the moving and follow paths | static |
| AI-03 | `checkDepth` over-counts by 1 and is throttled (57b124b) | CONFIRMED_BUG (fork regression, yor42 2025) | HIGH (+1); MEDIUM (downstream effects) | depth = `1 - frac` at the surface, computed every tick | +1 on every reading; stale for up to 7 ticks. Consumers: float thresholds (`EntityAIShipFloating.java:114-127`), `getShipFloatingDepth` 0.32, `ShipSkillHandler.java:161,255`, Hime models. Inferred: altered resting height or bobbing for every floating ship. Comments are stale. | Baseline depth values; throttle only if values are preserved | Ev. 4 (verified) |
| AI-04 | Path-follow arithmetic | CONFIRMED_BUG (inherited) | HIGH | Same (`ShipPathNavigate.java:504,354`) | `yOffset = pos2.x - pos1.x` (`ShipPathNavigate.java:467`); stuck recovery `dx < 0.2F` instead of `-0.2F` (`:337,339`) | Use y; use `-0.2F` | static |
| AI-05 | Path node type OPENABLE is unreachable | UNKNOWN_INTENT (inherited) | MEDIUM | Same | `pathset` is built but never read (`ShipPathFinder.java:56`); only FLUID or OPEN is returned (`:122`); the `getSafePoint` OPENABLE branch is dead. NYI cost malus at `:286`. | Not established | static |
| AI-06 | `travel` override; double travel per tick | UNKNOWN_INTENT | MEDIUM | `moveEntityWithHeading(strafe, forward)` with up = 0; same double call | `vertical` is now forwarded into `moveRelative`. `SWIM_SPEED` registered (the water-crash fix, f26fdc4). `updateShipNavigator` calls `travel` and then `super.onLivingUpdate()` calls it again. The airplane still passes 0. | Maintainer confirmation | static |
| AI-07 | Waypoint client hints (particles and coordinate label) removed | INCOMPLETE_PORT (candidate PLANNED_DEVIATION, e6b9edc) | MEDIUM | Particles to next/chest positions and an F/T/C label when holding a waypoint item, wrench or Pointer | No client branch (`TileEntityWaypoint.java:95-111`); only the model render (RND-06) | Restore, or approve the deviation | static |
| AI-08 | Auto emote (d7349f8, new) | UNKNOWN_INTENT | MEDIUM | None | Runs on every `ServerChatEvent` including cancelled ones (`EventHandler.java:772-777`). Substring matching. Default on. Opt-out is in-memory only, not persisted and never cleared. `findBestEmoteForMessageStrict` is unused. | Maintainer decides default, persistence and matching | static |

## Inherited upstream behavior (not a regression)
Historical check done (run 4 follow-up). The code path is HIGH confidence; gameplay equivalence is MEDIUM.

- Human decision (2026-10-13): the human said morale 0 at spawn is probably not intended. This is not a confirmation.
- Correction (historical research, 2026-10-13): morale 0 is not the spawn value.
  - The constructor default is morale 60 (`BasicEntityShip.java:167-176`, index `ID.M.Morale` = 30, `ID.java:527`). The same in `7c48a1e`, `d482e9c`, `3528989` and `8bca563`. 60 is inside the Exhausted tier (≤ `L_Tired` 900), so the HIT −4 also applies to a freshly spawned ship.
  - Morale 0 comes from `updateFuelState(nofuel)` (`BasicEntityShip.java:2768-2770`; `8bca563` `:3121`). It runs when the NoFuel flag is set and AI tasks exist. It sets morale 0 and clears the AI. A fresh ship has no grudge, so this runs on its first fuel check. This explains NoFuel=true at t=8 in run 4b. This is deliberate no-fuel behavior.
  - Thresholds (`ID.java:547-559`): L_Excited 5100, L_Happy 3900, L_Normal 2100, L_Tired 900, cap 16000. HIT tiers (`BuffHelper.java:676-682`): >5100 +4, >3900 +2, 2100-3900 none, 900-2100 −2, ≤900 −4.
  - Recovery: `updateMorale` runs every 128 ticks (`BasicEntityShip.java:1984`, `3195-3219`) and is skipped while NoFuel is set. Out of combat it adds +40 per call below 3100. From 0 to above 900 takes about 2,944 ticks (about 2.5 min); to above 2100, about 6,700 ticks (about 11 min). The comment at `:3200` says "take 5 min from 0 to 2100".
  - Spawn egg: `saveShipDataToNBT` does not save Morale. NBT load (`CapaShipSavedValues.java:152`) uses `getInteger`, so a missing key gives 0.
- Implication: the Exhausted-tier HIT −4 applies to the spawn default (60) as well as to morale 0. The 2.008 range is therefore not specific to the no-fuel zeroing.
- Human decision (2026-10-13, final): leave the starting morale as is. No change to the spawn default (60) or to the no-fuel zeroing. The earlier "probably not intended" remark is superseded by this decision.
- Status: UNKNOWN_INTENT, resolved by human decision (keep as-is). `feature-status.md` has no value for "inherited behavior kept by human decision". The closest fits were checked and rejected: PLANNED_DEVIATION requires a difference from history (here the behavior matches 8bca563), and CONFIRMED_WORKING requires runtime or test evidence of intended behavior (none). A status-model addition would need a separate decision; not proposed here.
- Observation only (not a finding): recovery from 0 to 2100 takes about 11 min at +40 per 128 ticks (about 6,700 ticks, per the correction above). The author's comment at `BasicEntityShip.java:3200` says "take 5 min from 0 to 2100". The two differ by about a factor of two. No change is planned; recorded so the difference is not rediscovered.
- (Superseded by the 2026-10-13 keep-as-is decision.) Candidate starting values and the constructor-default fix location were considered and are not pursued.
- Closed: the spawn default is not to be changed (human decision 2026-10-13). No next step for AI-10 beyond the harness adjustment.

| ID | Feature | Status | Conf. | Historical (8bca563) | Current (HEAD) | Desired (proposal) | Evidence |
|---|---|---|---|---|---|---|---|
| AI-10 | Range-target AI floor of 2 blocks plus Exhausted-morale HIT −4: a level-1 DDI in the Exhausted tier (spawn default 60; 0 after no-fuel zeroing) ignores targets beyond about 2 blocks | UNKNOWN_INTENT (inherited from 8bca563; not a 1.12.2 regression; resolved by human decision 2026-10-13: keep as-is; no status value for "kept by human decision") | HIGH (code path); MEDIUM (gameplay) | Same range rule: `updateRange` identical apart from formatting and `expand()`→`grow()` (`1d9f4a2`); `(int)` truncation and floor 2 present since `d482e9c` (2016-12-21); earlier `7c48a1e` (2016-11-24) used a 16-block floor. Morale tier table identical. | `EntityAIShipRangeTarget.updateRange` (lines 162-169) truncates with `(int)`, so `aiRange` = 2 for `attrRange` 2.008. Base DDI HIT 6F × 0.4F (`Values.java:32`); level-1 raw 6.008 (`BuffHelper.java:94`); morale tier id 3 "Exhausted" (morale ≤ `L_Tired` 900, includes 0; `BuffHelper.java:680`) applies HIT −4 (`Values.java:908-914`; `BuffHelper.java:676-682, 759`). 6.008 − 4 = 2.008. The spawn default is morale 60 (Exhausted tier), so the −4 applies from spawn; morale 0 (no-fuel zeroing) is a second source of the same tier. | Human decision 2026-10-13: keep the starting morale as is; no fix planned. The range limit stays as documented. The harness sets normal morale or places the husk within 2 blocks. | Harness run 4b: `ship-attacks-husk`, `-after-grudge`, `-close-no-teleport` FAIL (husk at 6, 6, 3 blocks; no target in 392 ticks); `-inside-range` PASS (husk at 1.2 blocks). Morale tier inferred from arithmetic (probe logged `attrRange` only). See `../reports/2026-10-13-gametest-harness.md` ("Historical check"). |

| ID | Reproduction | Severity | Depends on | Stabilize before Modern? | Candidate regression tests |
|---|---|---|---|---|---|
| AI-10 | Spawn a level-1 Destroyer I (default morale 60, Exhausted tier) or let it hit the no-fuel zeroing (morale 0); husk at 3 or 6 blocks; no target set. Repeat at morale above 2100 | LOW (gameplay; deliberate-looking design unknown) | morale (BuffHelper), `EntityAIShipRangeTarget` | no | Range-floor and morale-tier HIT arithmetic test |

## Suspected (static, untested)
These have no feature-status value yet (`feature-status.md` defines none for "suspected"). Do not treat them as CONFIRMED_BUG until a reproduction exists.

| ID | Feature | Status | Conf. | Historical (8bca563) | Current (HEAD) | Desired (proposal) | Evidence |
|---|---|---|---|---|---|---|---|
| AI-09 | Crafting task recipe read loops 9 times over the recipe tag list | Suspected (untested; no status applied) | LOW-MEDIUM | Not checked | `TaskHelper.java:153-160` loops `i < 9` and calls `tagList.getCompoundTagAt(i)` for each index. Each entry's `Slot` is then used to set a grid slot. Indexes past the end of a shorter list would read an empty compound with `Slot` 0 (from memory of 1.12.2 `NBTTagList` behavior, not verified), which would set grid slot 0 to EMPTY. A recipe with fewer than 9 entries that uses grid slot 0 may never craft. | Read the list by its real length; keep the grid slot when an entry is missing | static, lines re-read; logic not run |

### Per-record details

| ID | Reproduction | Severity | Depends on | Stabilize before Modern? | Candidate regression tests |
|---|---|---|---|---|---|
| AI-01 | Ship in guard-block mode (`GuardType` > 0, PassiveAI off), hostile mob within cannon range: no target is selected | HIGH | AI-02, `TargetHelper` | yes | Pure search-box function: the result strictly contains the bbox and extends 0.9r/0.6r; a target at 0.5r is found |
| AI-02 | Stationary guard with a target in range; measure the shot interval against `maxDelayTime` | MEDIUM | AI-01 | yes (with AI-01) | Attack-timer model: N idle ticks give floor(N/maxDelay) shots, independent of evaluation rate |
| AI-03 | Ship in a 3-deep water column; log `getShipDepth()` on sampled ticks; compare with the baseline formula | HIGH | Floating AI, skills, rendering | yes (depth contract before extraction) | Pure depth function over a liquid/air column; 0 immediately after leaving liquid; throttle-invariance |
| AI-04 | Path across a vertical offset; trigger stuck recovery with a target straight ahead | MEDIUM / LOW | `ShipPath` | no, but fix before extracting the navigator | `isDirectPathBetweenPoints` grid fixture; displacement-sign test |
| AI-05 | Closed wood door or fence gate on a route | LOW | `EntityAIShipOpenDoor` | no | `getPathType` characterization on door, gate, fence, lily pad, iron door |
| AI-06 | Ship in water with a move target; log `motionY`; compare with an airplane | MEDIUM | `ShipMoveHelper` | yes (capture a movement trace first) | Physics constants test of `moveEntityWithHeading`; runtime 10-block water path |
| AI-07 | Hold the Target Wrench near a linked waypoint pair | LOW | `ParticleHelper`, RND-06 | no | Manual checklist |
| AI-08 | Chat a matching word; toggle `/autoemote`; relog | LOW | `S2CSpawnParticle` | no | Matcher and cooldown unit tests |
| AI-09 | Craft with a recipe paper whose Recipe list has fewer than 9 entries and a planks item in grid slot 0; compare the result with the same recipe placed in slot 1 (the run 4 harness used slots 1 and 4, which avoided this) | LOW-MEDIUM (if reachable) | `TaskHelper` crafting path, ATTR-03 | no | Recipe-list parse with a short list keeps slot 0 |

## Assumptions
- The vanilla 1.12 `EntityAITasks` tick rate is 3 for non-running tasks (AI-02 cadence estimate). From memory, not verified.
- `setAIMoveSpeed` also sets `moveForward` in 1.12 (from memory).
- The double-`travel` structure works because vanilla zeroes `moveForward` earlier in the tick (inference).

## Unknowns
- No runtime evidence for any record.
- The intended guard cadence (AI-02); the intent of AI-05, AI-06 and AI-08.
- Task (crafting, mining, fishing, cooking) per-slot semantics beyond the diff; ATTR-03 found a duplication bug in a helper these tasks call.
- The README's "Y-axis angle too large prevents hit" fix (misaka10843 PR #2) maps to commits touching `EntityAIShipGuarding`/`ServerProxy` (7296046, a6076e3, 0ec3a8c, b812e8f, b2286f6). The same commit series introduced AI-01 and AI-02, so the "fix" itself needs re-verification.

## Risks
- AI-01 and AI-02 together disable or cripple guard-mode combat, a core feature.
- AI-03 changes float and depth behavior for every ship and may be misread as a rendering bug.
- AI-01 recreates the 1.10→1.12 `expand` vs `grow` trap that KCFindstr fixed in 1d9f4a2.

## Minecraft-independent logic candidates
`ShipPathHeap`, `ShipPathPoint`, the A* loop in `ShipPathFinder` (behind a block-access abstraction); `ShipMoveHelper.limitAngle`; `moveEntityWithHeading` drag and speed constants; `EntityAIShipFloating` motion and depth thresholds; `checkDepth` over a column abstraction; guard and follow range math; `updateAttackParms`; `wpStayTime2Ticks`; formation math; auto-emote matcher and cooldown.

## Recommended next actions
1. Stabilization candidates: AI-01 + AI-02 (one guard task), AI-03.
2. Maintainer decisions: AI-05, AI-06, AI-07 (deviation?), AI-08 defaults.
3. Runtime pass: guard-block combat, water floating, waypoint hints.

## Phase 3 updates
- 2026-10-13 AI-09 added as a static suspicion from the crafting-task review. The run 4 harness avoided the case (recipe in grid slots 1 and 4). See `../reports/2026-10-13-gametest-harness.md`.
- 2026-10-13 AI-10 added (upstream behavior, historical check done). Range-target AI floor of 2 blocks plus the Exhausted-morale HIT −4 makes a level-1 DDI in the Exhausted tier (spawn default 60, or 0 after no-fuel zeroing) ignore targets beyond about 2 blocks. Evidence and verdict are in `../reports/2026-10-13-gametest-harness.md` ("Historical check").
