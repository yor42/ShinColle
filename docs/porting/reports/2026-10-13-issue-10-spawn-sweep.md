# Issue #10 (ENT-13): Friendly ships deleted when animal or NPC spawning is off

- Date: 2026-10-13
- Phase: 3
- Author: Claude Code (documentation worker, Haiku; evidence supplied by the parent, which ran the implementer, unit-test and dedicated-harness steps; a regression-verifier is re-checking the final `@WrapWithCondition` version at the time of writing)
- Related issue/ADR: issue #10 (ENT-13; the issue title and state were not supplied and were not checked by this report). Gap `2026-10-13-entity-lifecycle.md` (ENT-13 entry under "Phase 3 updates").

## Status of the fix

**Verified, uncommitted.** The Mixin fix (final `@WrapWithCondition` version) and the `use_mixins=true` build change are in the working tree and are not committed. The regression-verifier checked the final snapshot (HEAD `7b91e2d` plus the working tree); its results are in "Final verification" below. Single-player was not re-run for the final version.

The fix is verified in the dev environment, and the reobfuscated jar has been run on a production-style dedicated server for patch application only. Ship survival on that server is not tested (blocked by GitHub #11; see "Production server check" and Unknowns).

## Final verification (regression-verifier, final snapshot)

Snapshot: HEAD `7b91e2d` plus the uncommitted working tree (the `@WrapWithCondition` Mixin fix, the `use_mixins=true` build change, and the `SpawnSweepGuard` unit tests). Results as reported by the verifier:

| Check | Command or input | Result |
|---|---|---|
| Unit tests | `./gradlew.bat --offline test --rerun-tasks` | BUILD SUCCESSFUL, exit 0. 21 tests, 0 failures, 0 errors, 0 skipped (PacketAuthorizationTest 3, TestEnvironmentTest 2, EntityIdGuardTest 5, InventoryHelperTest 6, SpawnSweepGuardTest 5) |
| Build | `./gradlew.bat --offline build` | BUILD SUCCESSFUL, exit 0 |
| Reobfuscated jar (1231 entries) | inspection | Manifest `MixinConfigs: shincolle.mixin.json`. Refmap: `setDead` to `func_70106_y()V`, `updateEntityWithOptionalForce` to `func_72866_a(Lnet/minecraft/entity/Entity;Z)V`. 0 gametest entries, 0 `org/spongepowered` entries, 0 `com/llamalad` entries. `shincolle.mixin.json` is identical in `src/main/resources` and in the jar, and lists `MixinWorldServer` (confirms it was restored after the red run) |
| Mixin review | code inspection | Import `com.llamalad7.mixinextras.injector.v2.WrapWithCondition`. Handler signature `(Entity)` returning boolean. Both `setDead` calls are covered (animal/water-mob branch and NPC branch). Stacks with other mods' wrappers |
| MixinExtras runtime | dedicated server log (verifier's run) | MixinExtras 0.5.5 confirmed loaded at runtime |
| Dedicated harness, spawn-off red | `gametest-dedicated-issue10-wrap-spawnoff-red-results.txt` | 3 passed, 2 failed; `ships=0`. **No server log kept for this run** (see Evidence gap below) |
| Dedicated harness, spawn-off green | `gametest-dedicated-issue10-wrap-spawnoff-green-results.txt` | 5 passed, 0 failed |
| Dedicated harness, normal | `gametest-dedicated-issue10-wrap-results.txt` | 8 passed, 0 failed |
| Diff scope | inspection | Clean; no debug leftovers; stash empty; `server.properties` back to spawn-on |

Not run by the verifier: `runClient` and a live server. The reobfuscated jar was run later on a production-style server (see "Production server check").

Evidence gap (wrap red run): the spawn-off red run of the `@WrapWithCondition` version kept only its results file, not a server log. The ship's death in that run rests on `ships=0`. The earlier `@Redirect` red run's server log shows `isDead=true ticks=1`, which is the direct evidence for the death mechanism; the `@WrapWithCondition` red run does not have an equivalent log.

## Production server check (2026-10-13)

Setup (parent-supplied):
- Cleanroom 0.6.13-alpha server installed at `C:\cleanroom server` (`cleanroom-0.6.13-alpha.jar`, `minecraft_server.1.12.2.jar`, `libraries/`).
- A copy was made in `run/server-prod-check/` (untracked). Its `mods/` held only the reobfuscated `shincolle-1.12.2.1.11.jar`.
- `server.properties`: `spawn-animals=false`, `spawn-npcs=false`, server port `127.0.0.1:25622`, RCON `25632`.
- Launch: `java -Dmixin.debug.verbose=true -Dmixin.debug.export=true -jar cleanroom-0.6.13-alpha.jar nogui`. On first start the server asked for `/fml confirm` (the Fugue/Scalar warning); it was answered through stdin.

Results:
- Log lines: `[CleanMix]: Preparing config shincolle.mixin.json`; `[CleanMix]: Mixing MixinWorldServer from shincolle.mixin.json into net.minecraft.world.WorldServer`; MixinExtras 0.5.5 initialised.
- `javap` of the exported `WorldServer.func_72866_a`: both `Entity.func_70106_y` (`setDead`) calls are guarded by `wrapWithCondition$...$shincolle$allowSweep(Entity)`. This shows the SRG refmap is in effect in production.
- Control: `/summon cow` and `/summon pig` were removed within about one tick with `spawn-animals=false`.
- **Ship survival on the production server was NOT tested.** `/summon shincolle:EntityDestroyerI` throws an NPE at `CapaShipSavedValues.java:129` (`loadNBTData`, called from `BasicEntityShip.readFromNBT`, `BasicEntityShip.java:585`) because the ShipExtProps NBT is missing. Filed as GitHub issue #11 (https://github.com/yor42/ShinColle/issues/11). Gap ENT-15 in `2026-10-13-entity-lifecycle.md`.
- Logs: `docs/porting/reports/logs/2026-10-13-issue10-prod-server.log` and `docs/porting/reports/logs/2026-10-13-issue10-prod-results.txt`.

Compatibility-level follow-up (human-approved):
- The first production run warned: "Class version 69 required is higher than the class version supported by the current version of Mixin (JAVA_8 supports class version 52)".
- `shincolle.mixin.json` `compatibilityLevel` changed from `JAVA_8` to `JAVA_25`. CleanMix 0.7.2's `CompatibilityLevel` enum goes from `JAVA_6` to `JAVA_25`; `JAVA_25` is class version 69, which matches the build.
- `test` and `build` both exited 0 (`docs/porting/reports/logs/2026-10-13-issue10-compat-build.log`).
- Production re-run: the warning appeared 0 times; the mixin still applied (both guards still in `func_72866_a`); the cow and pig were still removed. Logs: `docs/porting/reports/logs/2026-10-13-issue10-prod-server-compat.log` and `docs/porting/reports/logs/2026-10-13-issue10-prod-compat-results.txt`.
- Dev spawn-off harness re-run with `JAVA_25` (fresh world, parent-supplied): `SUMMARY passed=5 failed=0`; `dedi-spawnoff-ship-survives` passed. The server log has 0 "Class version" warnings and no mixin config errors. The only warnings are the two known dev ones (config not in a jar; refmap not read in dev). Results: `docs/porting/reports/logs/2026-10-13-gametest-dedicated-issue10-compat-spawnoff-results.txt`. The dev server log for this run was not reported to the documentation worker and is not cited here.

Decision (human, 2026-10-13): the human accepted the current proof for #10, namely the production patch shown in the bytecode and log, plus the dev-harness ship survival. Ship survival on the production server is not required for this acceptance and is not claimed. This is a human decision recorded as given; it is not a verification claim.

What this check does and does not show:
- Shows: the reobfuscated jar applies the exemption on a production-style Cleanroom 0.6.13 server (bytecode and log), with and without the `JAVA_25` change. The dev harness with `JAVA_25` passed 5/0.
- Does not show: that a ship survives the sweep on a production server (the NPE in #11 blocks that check via `/summon`).

## Scope

What was examined:
- The vanilla deletion path `WorldServer.updateEntityWithOptionalForce` (as mapped in the Cleanroom 0.6.10 source, `WorldServer.java:847`; the repo builds with 0.6.13, so line numbers may differ).
- The superclass chain of `BasicEntityShip` and its siblings (historical and current), from the gap entry ENT-13.
- The server settings `spawn-animals` and `spawn-npcs` as read by `DedicatedServer` and forced by `IntegratedServer` (gap ENT-13 evidence).
- `BasicEntityShip.setDead` persistence write (`BasicEntityShip.java:4342-4350`).
- The Mixin fix: `utility/SpawnSweepGuard`, `mixin/MixinWorldServer`, `shincolle.mixin.json`, the build configuration and the harness-only server arguments.
- The unit tests (`SpawnSweepGuardTest`) and the dedicated-server harness runs listed under Evidence.

Not covered:
- Whether a deleted ship's `CacheDataShip` copy is saved to disk or recoverable (U-ENT13-1 in the gap file).
- Runs of the reobfuscated jar on a real dedicated server (no run recorded).
- Coexistence with another mod that wraps the same call (untested).
- Historical behavior on 1.7.10 and 1.10.2 servers (inference only; no runtime test).
- Manual multiplayer with a real client.
- Any change to the superclass or to ship NBT.

## Evidence

1. Historical superclasses (upstream PinkaLulan/ShinColle, `mc-1.7.10` head 9312511 and `mc-1.10.2` head 4546ff5): `BasicEntityShip extends EntityTameable` in both. Gap ENT-13 records this as HIGH confidence.
2. Repo history: the same `BasicEntityShip` superclass from 7c48a1e (2016-11-24) through 0aad501 (the 1.12.2 init commit) to HEAD (`BasicEntityShip.java:73`). Gap ENT-13, HIGH confidence.
3. Vanilla deletion precedent (secondary, third-party MCP dumps): `WorldServer.updateEntityWithOptionalForce` deleted `EntityAnimal`, `EntityWaterMob` and `INpc` entities when the matching spawn setting was off, in 1.7.2 and 1.9 (https://github.com/xgraza/nebula-1.7.2/blob/1c010901ce9733fc73a70d34c7a1a67a356d2851/src/main/java/net/minecraft/world/WorldServer.java lines 632-645; https://github.com/CircuitLord/Minecraft-1.9-MCP/blob/bc89baf1fd0b5d422478619e7aba01c0b23bd405/src/minecraft/net/minecraft/world/WorldServer.java lines 833-845). Gap ENT-13, MEDIUM-HIGH confidence.
4. Upstream never had a workaround (no mixin, no coremod, no spawn-animals handling). Upstream issues #26, #30 and #40 report ships vanishing on servers, with no diagnosis (link to this bug Unknown). Gap ENT-13.
5. Current 1.12.2 deletion path: Cleanroom 0.6.10 mapped source `WorldServer.java:847`-area (`updateEntityWithOptionalForce`), calling `setDead` on `EntityAnimal` on the first tick when `spawn-animals=false`. Gap ENT-13 (stack trace and one-variable flip on the harness, dedicated server, HIGH).
6. Settings: `DedicatedServer.java:162-163` reads `spawn-animals` and `spawn-npcs` with a default of `true`. `IntegratedServer.java:170-171` forces both to `true` (single-player and LAN not affected). Gap ENT-13.
7. Persistence: `BasicEntityShip.setDead` (`BasicEntityShip.java:4342-4350`) writes a `CacheDataShip` with `isDead=true` and the full NBT through `ServerProxy.setShipWorldData`. Gap ENT-13.
8. Build change (parent-supplied; approved by the human): `gradle.properties` `use_mixins=true`. No new dependency: Cleanroom already provides CleanMix 0.7.2, Mixin 0.8.7 and MixinExtras 0.5.5.
9. Mixin loading (parent-supplied): jar manifest entry `MixinConfigs: shincolle.mixin.json` (`build.gradle:259`). For dev only, the harness-only `gametestServerArgs` passes `-Dcrl.dev.mixin=shincolle.mixin.json`. The `-Pgametest_spawnoff=true` option writes `spawn-animals=false` and `spawn-npcs=false` for the server.
10. Code (parent-supplied):
    - `src/main/java/com/lulan/shincolle/utility/SpawnSweepGuard.java`, `isExempt(Class entityClass, Class shipBase)` returns `shipBase.isAssignableFrom(entityClass)`.
    - `mixin/MixinWorldServer`, a MixinExtras `@WrapWithCondition` on the `Entity.setDead()` INVOKE inside `WorldServer.updateEntityWithOptionalForce`, covering both the animal/water-mob branch and the NPC branch. Only `BasicEntityShip` subclasses are exempt.
    - `src/main/resources/shincolle.mixin.json` (`required: true`, refmap `mixins.shincolle.refmap.json`). The `compatibilityLevel` was `JAVA_8` when Evidence 10 was recorded; it is now `JAVA_25` (see Evidence 17).
    - The first version used `@Redirect`. It was switched to `@WrapWithCondition` so that it stacks with other mods' wrappers instead of failing hard.
11. Refmap (parent-supplied): `setDead` maps to `func_70106_y()V`; `updateEntityWithOptionalForce` maps to `WorldServer.func_72866_a(Lnet/minecraft/entity/Entity;Z)V`. The reobfuscated jar has no shaded `org/spongepowered` or `com/llamalad` classes and no gametest entries.
12. Unit tests, `SpawnSweepGuardTest` (5 tests):
    - Red run: `docs/porting/reports/logs/2026-10-13-issue10-red.log`. Exit code 1 (as reported by the parent): `21 tests completed, 2 failed`; `shipIsExempt` fails at `SpawnSweepGuardTest.java:28`, `shipSubclassIsExempt` at `:33`.
    - Green run: `docs/porting/reports/logs/2026-10-13-issue10-green.log`.
    - Build logs: `docs/porting/reports/logs/2026-10-13-issue10-build.log` and `docs/porting/reports/logs/2026-10-13-issue10-wrap-build.log`.
    - Verifier run on the `@Redirect` version (`test --rerun-tasks`, as reported by the parent): 21 tests, 0 failures (`PacketAuthorizationTest` 3, `TestEnvironmentTest` 2, `EntityIdGuardTest` 5, `InventoryHelperTest` 6, `SpawnSweepGuardTest` 5); build passing. The verifier's logs are not in this repository.
    - The final `@WrapWithCondition` version of the unit tests: 21 tests, 0 failures (regression-verifier, HEAD `7b91e2d` plus working tree; see Final verification).
13. Dedicated harness, `@Redirect` version (parent-supplied):
    - Spawn-off red, mixin list emptied: 3 passed, 2 failed; the ship reads `isDead=true ticks=1 shipUID=-1`, `ships=0`. Logs: `docs/porting/reports/logs/2026-10-13-gametest-dedicated-issue10-spawnoff-red-results.txt` and `-spawnoff-red-server.log`.
    - Spawn-off green: 5 passed, 0 failed; `dedi-spawnoff-ship-survives ... shipUID=100`. Logs: `...-spawnoff-green-results.txt` and `-spawnoff-green-server.log`.
    - Normal dedicated: 8 passed, 0 failed. Log: `docs/porting/reports/logs/2026-10-13-gametest-dedicated-issue10-results.txt`.
    - Single-player run 10: 14 passed, 0 failed. Log: `docs/porting/reports/logs/2026-10-13-gametest-run10-results.txt`.
14. Dedicated harness, `@WrapWithCondition` version (parent-supplied):
    - Spawn-off red: 3 passed, 2 failed; `ships=0`. The red run disabled the mixin with `sed` on the json `mixins` array, then restored it (confirmed by the verifier). Log: `docs/porting/reports/logs/2026-10-13-gametest-dedicated-issue10-wrap-spawnoff-red-results.txt`. **No server log was kept for this run**, so the ship's death rests on `ships=0`; the `@Redirect` red server log (Evidence 13) is the only server-side record of `isDead=true ticks=1`.
    - Spawn-off green: 5 passed, 0 failed. Log: `...-wrap-spawnoff-green-results.txt`.
    - Normal dedicated: 8 passed, 0 failed. Log: `docs/porting/reports/logs/2026-10-13-gametest-dedicated-issue10-wrap-results.txt`.
    - Single-player was not re-run for the wrap change; the integrated server forces both settings on, so it is not affected by this path.
15. Attempt history: one earlier `runClient` attempt hung with no output; the identical rerun passed. Cause unknown (Unknowns).
16. Production-style dedicated server (parent-supplied; see "Production server check"): Cleanroom 0.6.13-alpha, reobfuscated jar, `spawn-animals=false`, `spawn-npcs=false`. Log `docs/porting/reports/logs/2026-10-13-issue10-prod-server.log`; results `docs/porting/reports/logs/2026-10-13-issue10-prod-results.txt`. Shows CleanMix applying `MixinWorldServer`, `javap` of `func_72866_a` with both `setDead` calls guarded, and cow and pig removed. Ship survival not tested (NPE, GitHub #11).
17. Compatibility-level change (parent-supplied, human-approved): `shincolle.mixin.json` `JAVA_8` to `JAVA_25`, after a production warning about class version 69. `docs/porting/reports/logs/2026-10-13-issue10-compat-build.log` (test and build exit 0). Production re-run with `docs/porting/reports/logs/2026-10-13-issue10-prod-server-compat.log` and `docs/porting/reports/logs/2026-10-13-issue10-prod-compat-results.txt`: warning 0 times, mixin applied, cow and pig removed. The dev spawn-off harness re-run with `JAVA_25` is pending.

## Confirmed findings

| Feature | Status | Confidence | Historical | Current 1.12.2 | Desired | Evidence |
|---|---|---|---|---|---|---|
| Friendly ships (`BasicEntityShip` and subclasses) deleted by the spawn sweep when `spawn-animals=false` or `spawn-npcs=false` | CONFIRMED_BUG (pre-fix, dedicated harness). Post-fix: verified, uncommitted (dedicated harness and unit tests in the dev environment); not CONFIRMED_WORKING on a real server | HIGH for the pre-fix deletion (harness); MEDIUM for the post-fix dedicated result (harness only; one run per configuration) | Superclass unchanged since 7c48a1e; 1.7.10 and 1.10.2 loss of ships inferred only (Evidence 1, 3, 4) | Deleted on first tick (Evidence 5, 13); the `@Redirect` and `@WrapWithCondition` versions keep the ship in the harness (Evidence 13, 14) | Ships protected from the sweep via a Mixin exemption; the human's decision (2026-10-13) departs from upstream | 1-7, 10-14 |
| Settings reads and forced values | Recorded code behavior (not a feature status) | HIGH (source read) | n/a | Dedicated defaults `true`; integrated forces `true` (Evidence 6) | n/a | 6 |
| Ship persistence on `setDead` | Unknown whether recoverable | n/a | n/a | Writes `CacheDataShip` with `isDead=true` (Evidence 7) | Unknown | 7 |
| Build: Mixin enabled | Build change, human-approved | MEDIUM (parent-supplied build inspection; the build logs are the record) | n/a | `use_mixins=true`; no new dependency (Evidence 8) | n/a | 8, 9, 11 |
| Mixin patch applied on a production-style server | Patch application verified on one server run (bytecode and log); ship survival NOT verified there | MEDIUM (one run per configuration; the JAVA_25 production re-run and the dev JAVA_25 harness re-run are recorded) | n/a | Cleanroom 0.6.13 server: `func_72866_a` guards in bytecode; cow and pig removed (Evidence 16, 17) | Ships survive the sweep (human-accepted proof: patch plus dev-harness survival; Production server check) | 16, 17 |
| Summon of a ship from `/summon shincolle:EntityDestroyerI` on production | CONFIRMED_BUG (GitHub #11, tracked as ENT-15 in the gap file) | HIGH (reproduced on the production server) | Unknown whether upstream `/summon` worked | NPE at `CapaShipSavedValues.java:129` (see "Production server check") | Unknown (not decided here) | 16 |

Note: the pre-fix deletion is confirmed by the harness only. Its one-variable flip and stack trace are recorded in the gap entry; the `spawn-off red` run in both versions reproduces it.

## Assumptions

- A-1: The vanilla sweep (`updateEntityWithOptionalForce`) is the only deletion path for friendly ships in this report. Other despawn and removal paths were not checked.
- A-2: Mounts (`EntityCreature`), summons (`EntityLiving`) and hostile ships (`EntityMob`) are not `EntityAnimal`, so the animal branch does not delete them. Derived from the superclass chain; not tested.
- A-3: The wrapped `Entity.setDead()` call inside the sweep is the only call that needs the exemption. Derived from the code described in Evidence 10; not checked against other paths.
- A-4: The harness-forged path exercises the same server deletion code as a real server with the same settings. Patch application is now seen on a production-style server; ship survival is not (U-12).
- A-5: The 1.7.10 and 1.10.2 loss of ships is inferred from the vanilla precedent and the upstream issues, not from a run (ENT-13).

## Unknowns

- U-1: Whether a deleted ship's `CacheDataShip` copy (written by `setDead`) is saved to disk or recoverable. Resolves by a targeted read of the save path and a run that inspects the saved data.
- U-2: Partly resolved. The reobfuscated jar on a production-style Cleanroom 0.6.13 server is verified for patch application (bytecode and log). Ship survival on that server is not verified (the NPE in GitHub #11 blocks the `/summon` test). Resolves by a ship-survival run once #11 is fixed or by another ship creation path that does not hit the NPE; needs human go-ahead.
- U-12: Ship survival of the sweep on the production server. Not tested; blocked by #11. Human accepted the patch-plus-dev-proof decision for #10 (see "Production server check"), so this is an open verification gap, not an acceptance blocker by the human's account.
- U-13: Resolved. The dev spawn-off harness re-run with `JAVA_25` passed 5/0 with no "Class version" warnings (see Production server check). The dev server log for that run is not cited here.
- U-3: Whether the mixin is applied in the built jar. No explicit "mixin applied" log line exists; the evidence is behavioural only (harness).
- U-4: Coexistence with another mod that wraps the same `setDead` call. Untested. The `@WrapWithCondition` form was chosen to allow stacking.
- U-5: Resolved. The final `@WrapWithCondition` version: 21 tests, 0 failures (see Final verification).
- U-11: Evidence gap. The spawn-off red run of the `@WrapWithCondition` version has no server log; its death evidence is `ships=0` only. Resolves by a red run that keeps its server log (requires a dev harness run; needs human go-ahead).
- U-6: Cause of the one earlier `runClient` attempt that hung with no output. The identical rerun passed.
- U-7: Whether the upstream issues #26, #30 and #40 are this bug.
- U-8: Whether 1.7.10 and 1.10.2 servers actually lost ships (no runtime test).
- U-9: Whether the NPC branch affects any ship class (ships are known to be `EntityAnimal`; the NPC branch is covered by the wrap but its effect on ships is not separately tested).
- U-10: Whether a deleted ship's data (U-1) and the `isDead` flag interact with the exemption; not checked.
- Expected in dev, not a problem by itself: warnings about the missing refmap and about the config not being in a jar.

## Risks

- **Mixin dependency on the vanilla method (build and target risk).** The mixin targets a mapped method name and signature (`func_72866_a`, `func_70106_y`). A different Cleanroom mapping or a Cleanroom bump would break the target. Impact: the exemption fails to apply (the bug returns) or the mixin fails at load. Likelihood: low for this build; not checked against 0.6.13 line numbers. Mitigation: `required: true` makes a missing target fail loudly; any Cleanroom bump needs human approval (safety rules).
- **Build change (approved).** `use_mixins=true` changes the build configuration. Human-approved; no new dependency. Flag for the human: this is a build change.
- **Other mods wrapping the same call.** Possible conflict. `@WrapWithCondition` was chosen to stack; not tested (U-4).
- **Persisted data.** No NBT layout, registry ID or persistence format change is recorded by the parent. The deleted ship's `CacheDataShip` copy is unverified (U-1); the fix changes whether deletion happens, which may change what is saved for affected ships. Flag for review.
- **Behavior change on servers.** With the fix, friendly ships survive the sweep on servers that disabled animal or NPC spawning. This departs from upstream (human decision). Mitigation: documented in the gap entry.
- **Dev-only harness arguments.** `-Dcrl.dev.mixin=shincolle.mixin.json` and the json-editing harness steps are dev-only. The jar manifest carries the real `MixinConfigs` entry.
- **Single-player not re-run for the final version.** Low risk, since the integrated server forces both settings on; not verified for the final version.
- **Mixin compatibility level changed to JAVA_25 (human-approved).** `shincolle.mixin.json` `compatibilityLevel` moved from `JAVA_8` to `JAVA_25` to match the build's class version 69. This is a configuration change that targets the mixin runtime; the production re-run showed no warning and the patch still applied. Dev spawn-off re-run with `JAVA_25` passed 5/0 (U-13 resolved). Flag for review.
- **Production ship survival blocked.** `/summon` of a ship throws an NPE on production (GitHub #11, ENT-15), so ship survival on the production server cannot be checked through that path. Impact: the #10 proof on production is patch-level only (human-accepted). Likelihood: certain on the tested server.

## Recommended next actions

1. Done: the final verifier result for the `@WrapWithCondition` version is recorded (verified, uncommitted).
2. Run the built reobfuscated jar once on a real dedicated server with `spawn-animals=false` and `spawn-npcs=false`, and record the log path. Needs human go-ahead (it starts a server and may change the server world).
2a. Optionally repeat the `@WrapWithCondition` spawn-off red run with its server log kept (closes U-11). Needs human go-ahead (it changes the dev world).
2b. Done: the dev spawn-off harness result with `JAVA_25` is recorded (U-13 resolved).
2c. Ship survival on the production-style server (U-12): blocked by GitHub #11 (ENT-15). Needs a fix or another creation path, and human go-ahead before any server run.
3. Resolve U-1 (`CacheDataShip` persistence) with a read of the save path. Read-only; any change to persistence needs human approval.
4. Decide whether the coexistence risk (U-4) needs a test with another mod. Needs human decision.
5. Commit only when asked. The change is uncommitted; commit needs the human's go-ahead.
