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
