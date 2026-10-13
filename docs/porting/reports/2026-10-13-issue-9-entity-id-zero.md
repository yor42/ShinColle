# Issue #9 (NET-13): Entity lookups reject entity id 0

- Date: 2026-10-13
- Phase: 3
- Author: Claude Code (documentation worker; evidence supplied by the parent, which ran the implementer, unit-test and dedicated-harness steps; a regression-verifier was running at the time of writing)
- Related issue/ADR: issue #9 (NET-13; issue state not checked by this report). Gap `2026-10-13-network-sync.md` (NET-13 and NET-14).

## Status of the fix

**Verified, uncommitted.** The #9 fix and the #9b follow-up (below) are in the working tree and are not committed. The regression-verifier checked the final snapshot (HEAD `2403673` plus the working tree); its results are in the "Final verification" section below. The verifier's own logs are not in the log set of this repository; the results are recorded as the verifier reported them, and the runtime results are the saved dedicated and single-player logs.

## Final verification (regression-verifier, final snapshot)

Snapshot: HEAD `2403673` plus the uncommitted working tree (the #9 fix and the #9b follow-up).

| Check | Command | Result (as reported by the verifier) |
|---|---|---|
| Unit tests | `./gradlew.bat --offline test --rerun-tasks` | BUILD SUCCESSFUL, exit 0; 16 tests, 0 failures. Per class: PacketAuthorizationTest 3, TestEnvironmentTest 2, EntityIdGuardTest 5, InventoryHelperTest 6 |
| Build | `./gradlew.bat --offline build` | BUILD SUCCESSFUL, exit 0 |
| Jar contents | inspection of the main and dev jars | No gametest entries |
| Red/green logs | inspection | `logs/2026-10-13-issue9b-red.log`: `absentEntityEncodesToNoneAndIsRejected` fails at `EntityIdGuardTest.java:35`. Green: `logs/2026-10-13-issue9b-green.log` |
| Dedicated harness, fresh world | saved log | `logs/2026-10-13-gametest-dedicated-issue9b-results.txt`: `SUMMARY passed=8 failed=0`; owner eid 0 (line 15) |
| Single-player harness, #9 edits | saved logs | `logs/2026-10-13-gametest-run9a-results.txt`, `run9b-results.txt`, `run9c-results.txt`: each `SUMMARY passed=14 failed=0` |
| Diff review | inspection | No debug leftovers; the stash is empty; nothing under `run/` changed |

Not run by the verifier: `runClient` and a live server. The runtime results above are recorded evidence from earlier runs of the harness, not a new run by the verifier. The #9 unit-test counts (14 before #9b, 16 after) are consistent with the red and green logs.

## Scope
What was examined:
- `EntityHelper.getEntityByID` and `EntityHelper.getEntityPlayerByID` (the `> 0` check before and the `EntityIdGuard` call after the fix).
- The new `EntityIdGuard.isLookupable(int)` and its unit test.
- A grep of `src/main/java` for the call sites of both helpers and for `entityID > 0`-style checks, to classify which sites use Minecraft entity ids and which use ship or player UIDs (the classification is partly spot-checked; see Evidence 8).
- The dedicated-server harness run for issue #9, the earlier failing dedicated run in the same log folder, and the single-player harness run (run 7).

Not covered:
- Independent verification: done for the final snapshot (see "Final verification"). Verifier logs are not in this repository.
- Manual multiplayer with a real client. All dedicated runs use harness-forged packets.
- The call-site audit for 0 used as a sentinel: done for #9b (see Follow-up #9b). Its residual risk is listed under Risks.
- The other `entityID > 0` site, `S2CEntitySync.java:91`: fixed in #9b (see Follow-up #9b, R2). Gap NET-14 is recorded as fixed in #9.
- Git diff and commit state. Nothing is committed.
- The historical behavior of the `> 0` check in 8bca563 (not checked).

## Evidence
1. Pre-fix rule: `docs/porting/gaps/2026-10-13-network-sync.md:98` (NET-13 entry) states that `EntityHelper.getEntityPlayerByID` (`EntityHelper.java:294-303`) and `getEntityByID` (`:254-268`) required `entityID > 0`. The pre-fix source is not visible in the working tree; this text and the implementer's report are the only pre-fix source. Git HEAD was not inspected by this worker.
2. Current code (working tree): `src/main/java/com/lulan/shincolle/utility/EntityHelper.java:263` `if (world != null && EntityIdGuard.isLookupable(entityID)) {` and `:303` `if (world != null && EntityIdGuard.isLookupable(entityID)) {`. Confirmed by grep and Read.
3. `src/main/java/com/lulan/shincolle/utility/EntityIdGuard.java:12-14`: `public static boolean isLookupable(int entityId) { return entityId >= 0; }`. The class Javadoc (lines 3-6) says it applies to Minecraft entity ids only, not to ship or player UIDs, which use 0 or -1 as "none".
4. Unit test `src/test/java/com/lulan/shincolle/utility/EntityIdGuardTest.java`, three `@Test` methods: `entityIdZeroIsLookupable` (line 15-17, asserts `isLookupable(0)` is true), `positiveEntityIdIsLookupable` (lines 19-23, values 1 and 4096), `negativeEntityIdIsRejected` (lines 25-29, values -1 and `Integer.MIN_VALUE`).
5. Red run: `docs/porting/reports/logs/2026-10-13-issue9-red.log`. Output: `EntityIdGuardTest > entityIdZeroIsLookupable() FAILED` at `EntityIdGuardTest.java:16`; `14 tests completed, 1 failed`; `BUILD FAILED in 5s`. Command as reported by the implementer: `./gradlew.bat --offline test`. Exit code 1 as reported by the implementer; the log does not print an exit code.
6. Green run: `docs/porting/reports/logs/2026-10-13-issue9-green.log`. Two invocations in one log: `test` (`BUILD SUCCESSFUL in 7s`) and `build` (`BUILD SUCCESSFUL in 8s`, including `:jar`, `:remapJar`, `:sourcesJar`, `:assemble`, `:check`). Exit code 0 as reported by the implementer. The log does not print the number of tests that ran in the green `test` task.
7. Dedicated-server run (post-fix): `docs/porting/reports/logs/2026-10-13-gametest-dedicated-issue9-results.txt`.
   - Line 1: `PASS dedi-connect: joined 127.0.0.1:25621 as Developer, 18 mods`.
   - Line 12: `PASS dedi-spawn-ship-with-egg: egg ship owned by player UID 100 at spawn, then set to foreign owner 9999 (shortcut); ownerUID=9999 level=1 ...`.
   - Line 15: `INFO dedi-nonop-survival-forged-packets: forging CmdChOwner(owner eid 0, ship eid 8631) and CmdShipAttr(level 77, bonus 9 x6)`. The player's entity id is 0 in this run.
   - Lines 18, 26: non-OP survival and non-OP creative cases, both `PASS`, state unchanged (ownerUID=9999, level=1).
   - Line 35: `PASS dedi-op-commands: ... ownerUID=9999 level=1 ... -> ownerUID=100 level=42 bonus=10,11,12,13,14,15`.
   - Line 42: `PASS dedi-op-forged-packets-control: ... -> ownerUID=100 level=77 bonus=9,9,9,9,9,9`.
   - Line 46: `SUMMARY passed=8 failed=0`.
   - No dedicated server log for this run is in the log folder (only the results file). The server log cannot be cross-checked.
8. Guard classification (implementer's list), spot-checked by grep:
   - Changed: `EntityHelper.java:263` and `:303` (confirmed, Evidence 2).
   - `S2CEntitySync.java:91` `if (msg.entityID > 0) {` (confirmed). Kept unchanged. Out of scope for this fix; see NET-14.
   - `EntityHelper.java:274` `if (sid > 0)` in `getShipByUID` is a ship UID, not an entity id (confirmed).
   - `BasicEntityShip.java`: the `GuardID` and `PlayerEID` call sites are at lines 1768 and 1780 (the implementer cited the `if` lines 1767 and 1779). Both calls sit behind a `> 0` check, so the 0 sentinel never reaches the helper there (confirmed). The `-1` writes are at `:1804` and `:2911` (confirmed). The implementer's note that these default to 0 is consistent with the `> 0` guards.
   - `GuiDesk.java`: the call is at line 1012 (the implementer cited 1011; off by one).
   - `TeamHelper`, `InteractHelper`, `CapaTeitoku` (sentinel lines cited by the implementer): not re-checked by this worker.
9. Callers of the two helpers that have no `> 0` check in front of them (from grep of `src/main/java`; not classified individually): `S2CGUIPackets.java:217,228,241,294,401,427`; `S2CEntitySync.java:92,121,403,411,427,497,544`; `S2CSpawnParticle.java:166`; `ParticleHelper.java:961`; `CapaTeitoku.java:124,207`; `MetamorphHelper.java:72,471`; `ShipSkillHandler.java:495`; `ServerProxy.java:581,742`; `CommandHelper.java:178,240`; `ShipCmdGetShip.java:183`; `GuiDesk.java:1012`; and the `C2SInputPackets` and `C2SGUIPackets` calls listed in the grep output. Whether any of these uses 0 to mean "none" is not established (see Risks).
10. Single-player harness run 7: `docs/porting/reports/logs/2026-10-13-gametest-run7-results.txt`. A `^PASS` count returns 14 (lines 2, 5, 7, 8, 9, 30, 31, 58, 75, 92, 118, 125, 132, 133; line 118 is `ai10-morale0-ignores-far-husk`), no `FAIL` line, and `SUMMARY passed=14 failed=0` (line 135). The file does not record the build state, so whether run 7 ran with the fix is not established (see Unknowns). The single-player run does not test NET-13 directly.
11. Earlier dedicated run with a failure: `docs/porting/reports/logs/2026-10-13-gametest-dedicated-results.txt`. Line 7 `precondition op=false gm=SURVIVAL ships=0`; line 12 `FAIL dedi-spawn-ship-with-egg: ship not found or fakeowner failed: ships=0`; lines 14, 16, 18, 22 `FAIL ... precondition failed: expected exactly 1 ship on the server, got ships=0`; line 26 `SUMMARY passed=3 failed=5`. This file does not show a ship left over from a previous run (the precondition reads `ships=0`), so it does not support the explanation given in the parent's brief (see Unknowns).
12. Harness dedicated server directory and level: `build.gradle:192` `def serverDir = file('run/server-gametest')`. The level name in the dedicated server log of the issue #2 run is `world-gametest` (`docs/porting/reports/logs/2026-10-13-gametest-dedicated-final-server.log:227`).
13. Harness source: `src/gametest/java/com/lulan/shincolle/gametest/GameTestRunner.java:709-1174` contains the dedicated-mode steps and writes `dedicated-results.txt`. A search of `src/gametest` for pre-join padding code (`i < 8`, `dummy`, `filler`, `placeholder`, `pre-join`) returned no matches. This is consistent with the workaround having been removed, but it is not proof of removal.

## Follow-up #9b: sentinel regression and the sync packet (2026-10-13)

The #9 fix made `EntityHelper` accept id 0. That exposed two packet writers that used 0 as "no entity", so the follow-up fixes the senders as well as the receivers. Both sides run the same mod version, and nothing is persisted, so the wire change needs no migration. The human approved both changes, R1 and R2, as part of #9.

- **R1 (regression from the #9 fix).** The crane and waypoint GUI packets in `S2CGUIPackets.java` wrote `0` for "no owner" (around `:734-738` and `:764-768`). The receivers at `:217` and `:241` would then resolve that 0 to the first player on the server. The writers now call `EntityIdGuard.encodeOptional(tile2.owner, EntityPlayer::getEntityId)` (`S2CGUIPackets.java:735` and `:761`), which gives `NO_ENTITY` (-1) for null.
- **R2 (sync packet, NET-14).** `S2CEntitySync.java:92` now reads `if (EntityIdGuard.isLookupable(msg.entityID))` (before: `> 0`, at `:91`; one line was added above it). The writer at `:732-733` sends -1 for a null entity. This fixes the morph-host sync for a player with id 0.
- **Helper.** `EntityIdGuard.java` adds `NO_ENTITY = -1` and `encodeOptional(E, ToIntFunction<E>)` (`:9-10`, `:19-21`).
- **Tests.** `EntityIdGuardTest.java` now has five test methods (16 tests in the full suite): `absentEntityEncodesToNoneAndIsRejected` (`:32-38`) and `presentEntityWithIdZeroEncodesToZeroAndIsLookupable` (`:40-45`) are new.
- **Red run:** `docs/porting/reports/logs/2026-10-13-issue9b-red.log`. `EntityIdGuardTest > absentEntityEncodesToNoneAndIsRejected() FAILED` at `EntityIdGuardTest.java:35`; `16 tests completed, 1 failed`; `BUILD FAILED in 4s`. Exit code as reported by the implementer.
- **Green run:** `docs/porting/reports/logs/2026-10-13-issue9b-green.log`. `test` `BUILD SUCCESSFUL in 5s`; `build` `BUILD SUCCESSFUL in 8s`. Exit codes as reported by the implementer.
- **Dedicated harness, fresh world:** `docs/porting/reports/logs/2026-10-13-gametest-dedicated-issue9b-results.txt`. `SUMMARY passed=8 failed=0` (line 46). The player is still at entity id 0 (line 15: `forging CmdChOwner(owner eid 0, ship eid 8261)`). The OP control rows are at lines 35 (owner 9999 to 100, level 1 to 42) and 42 (owner 9999 to 100, level 77). The non-OP rows at lines 18 and 26 are not discriminating (see the Confirmed findings note).
- **Call-site audit (implementer, about 40 `getEntityByID` and `getEntityPlayerByID` call sites).** R1 was the only sender that used 0 as a sentinel. `S2CEntitySync.java:403`, `:411` and `:427` (mount and rider) also write 0 for "no mount". Their receivers keep their own `> 0` checks, so they do not regress. The side effect is that a vehicle with entity id 0 is ignored by those receivers. This worker did not re-check the audit line by line.
- **NBT fields not changed.** `BasicEntityShip` `GuardID` and `PlayerEID` default to 0 in NBT (`CapaShipSavedValues.java:51,146`). They are guarded by `> 0` at the call sites (`BasicEntityShip.java:1767-1768`, `:1779-1780`), so they were left as they are. No NBT layout change.
- Residual risk and open items after #9b: see Risks and Unknowns (the 0-sentinel audit item is now answered; the mount/rider side effect is the remaining note).

## Confirmed findings

| Feature | Status | Confidence | Historical | Current 1.12.2 | Desired | Evidence |
|---|---|---|---|---|---|---|
| Entity lookups by Minecraft entity id 0 (`EntityHelper.getEntityByID`, `getEntityPlayerByID`) | Pre-fix: CONFIRMED_BUG (static; see note). Post-fix: verified (uncommitted); CONFIRMED_WORKING at MEDIUM for the scripted dedicated case | MEDIUM | 8bca563 not checked | Working tree: `>= 0` via `EntityIdGuard` (uncommitted) | Lookups accept any non-negative Minecraft entity id | 1, 2, 3, 4, 5, 6, 7, Final verification |
| Non-OP forged owner/attribute packets rejected with the player at entity id 0 | Not discriminating for NET-13 on its own: the non-OP "unchanged" result would also occur if the lookup failed. Use the OP rows below as the entity-id-0 evidence | MEDIUM | Pre-issue #2: no server OP check (NET-02) | Post-fix: state unchanged (ownerUID 9999, level 1), lines 15 and 18 | Same | 7 |
| OP owner and attribute commands take effect with the player at entity id 0 | CONFIRMED_WORKING (one dedicated run, harness-forged packets; verified, uncommitted) | MEDIUM | n/a | owner 9999 → 100, level 1 → 42, bonus 10..15 (`/shipchangeowner`, `/shipattrs`, see line 35) | Same | 7 |
| OP forged-packet control at entity id 0 | CONFIRMED_WORKING (one dedicated run; verified, uncommitted) | MEDIUM | n/a | owner 9999 → 100, level 77, bonus 9×6 (line 42) | Same | 7 |
| Single-player harness (14 steps) has no regression in its scripted cases | Recorded result only (one run; build state not recorded) | MEDIUM | n/a | 14 PASS, 0 FAIL | n/a | 10 |

Note on the pre-fix row: the NET-13 gap entry records confidence HIGH. This report records MEDIUM for the pre-fix failure. The static reading (`> 0`, and the red unit test) is consistent, but no saved pre-fix dedicated run with the player at entity id 0 exists in this log set. The earlier dedicated runs either used the eight-entity pre-join workaround (issue #2 run, `dedicated-final`, owner `eid 8`) or failed before the forged step (Evidence 11). The gap entry's HIGH is not changed by this report; the parent should decide which confidence to keep.

The pre-fix "rejected" behavior is inferred from the code and the NET-13 entry, not from a saved log. The post-fix dedicated run shows the player at id 0 being accepted (Evidence 7).

## Assumptions
- The working tree differs from HEAD only by the `EntityHelper` change and the new `EntityIdGuard` and test (from the `git status` snapshot at session start, plus the implementer's report). This worker did not run `git diff`.
- The pre-fix predicate was `entityID > 0`, as NET-13 records (Evidence 1).
- A first player on a fresh dedicated server gets entity id 0. This was observed once (Evidence 7, line 15). General behavior of the entity id counter was not checked in source here.
- The harness-forged packets exercise the same server-side path as a real client packet. The same assumption is recorded in the issue #2 report.
- The implementer's guard classification (Evidence 8) is correct for the sites not re-checked.

## Unknowns
1. **Independent verification result.** Resolved: the verifier's final-snapshot results are in "Final verification". Its logs are not in this repository's log set.
2. **Callers that use 0 as "none".** Answered in #9b: the implementer's audit found R1 as the only sender that used 0 as a sentinel; R1 is fixed. The audit was not re-checked line by line by this worker. The mount and rider writers (`S2CEntitySync.java:403,411,427`) still write 0 for "no mount"; the side effect is noted under Follow-up #9b.
3. **Ship that holds id 0.** Whether a ship spawned before any player on a fresh server can hold entity id 0. Not tested. With #9b the sync check (`S2CEntitySync.java:92`) accepts id 0, so a ship with id 0 is no longer dropped there; this is not verified by a run.
4. **Failing earlier dedicated run.** The parent's brief says a first attempt failed five steps because a ship from the previous run persisted. The saved file `logs/2026-10-13-gametest-dedicated-results.txt` shows `ships=0` at the precondition and after the egg step (Evidence 11), so it does not show a persisted ship. Either the explanation applies to a run whose log was not saved, or it is wrong. Resolves by saving the failing run's log or by a fresh rerun with `run/server-gametest` and `world-gametest` deleted first.
5. **Run 7 build state.** Whether `2026-10-13-gametest-run7-results.txt` was produced with the fix in place. The file does not record it. Resolves by the run's log or by the parent's confirmation of the run time.
6. **Exit codes.** The red exit code (1) and green exit code (0) are as reported by the implementer. The logs show `BUILD FAILED` and `BUILD SUCCESSFUL` but no exit code. Resolves by the verifier's captured exit codes.
7. **Manual multiplayer.** A real client joining a dedicated server as the first player was not run.
8. **Historical 8bca563 behavior** of the `> 0` check. Not checked.
9. **Classification not re-checked:** `TeamHelper`, `InteractHelper`, `CapaTeitoku` sentinel lines (implementer's list). Resolves with a read of those lines.

## Risks
- **Behavior change for 0-as-"none" callers.** Was the main risk before #9b. R1 (crane and waypoint owner) was the concrete case and is fixed by `encodeOptional`. Residual: the mount and rider writers (`S2CEntitySync.java:403,411,427`) send 0 for "no mount", and a vehicle with entity id 0 is ignored by their receivers. Impact: low (a ride-state lookup for one entity id); likelihood: depends on the id counter (Unknown 3). Mitigation: the same `encodeOptional` pattern, if a human approves it.
- **Ship owner and attribute path.** The OP fix (issue #2) and this fix both depend on the player being found by id. A regression in either would show in the dedicated harness, not in the unit test alone.
- **Harness state.** A dedicated run needs a fresh `run/server-gametest` and `world-gametest` (see the harness note). A stale world can make the single-ship precondition fail (Unknown 4).
- **Wire values and persisted data.** The packet layout is unchanged, but the value written for "no entity" changed from 0 to -1 in `S2CGUIPackets` (`:735`, `:761`) and `S2CEntitySync` (`:732-733`). Both sides run the same mod version and nothing is persisted, so no migration is needed. NBT layout, registry IDs, dependencies and target versions are unchanged (implementer's report).
- **Other `entityID > 0` sites.** `S2CEntitySync.java:92` (formerly `:91`) is now `isLookupable`, so sync packets for entity id 0 are accepted (#9b, R2).

## Recommended next actions
1. Done: the regression-verifier's final-snapshot results are recorded (verified, uncommitted). Next is the commit, which the human has asked for; commit needs the human's go-ahead as in the safety rules.
2. Done in #9b: the 0-sentinel call-site audit. Remaining: a line-by-line check of the mount and rider writers (`S2CEntitySync.java:403,411,427`), read-only. Any change needs approval.
3. NET-14 (`S2CEntitySync.java`) is fixed in #9b (R2), verified, uncommitted. Any further change to the mount/rider packets needs human approval.
4. Save the failing dedicated run's log (or rerun with a fresh server and world) so Unknown 4 can be closed. Running the dedicated harness starts a client and a server and changes the dev world; it needs human go-ahead.
5. Commit only when asked. The change is uncommitted. Needs approval for any commit.
