# Agent Work Batches: Open `ready-for-agent` Issues

- Date: 2026-10-14
- Phase: 3 (Legacy Stabilization)
- Author: Claude Code (sonnet-implementer, planning only; no code, GitHub or label changes)
- Related issue/ADR: ADR-0002 (save compatibility), ADR-0003, ADR-0004 (server-authoritative packets); `2026-10-14-stabilization-findings.md`; `2026-10-14-optimization-and-health-review.md`; decisions P-7, P-10, P-13 to P-17 in `../decisions/README.md`

## Scope
Groups the 31 open GitHub issues labelled `ready-for-agent` (queried with `gh issue list --state open --label ready-for-agent`, 2026-10-14) into 13 batches that one `sonnet-implementer` session can finish and one `regression-verifier` can verify. Each issue is in exactly one batch (checked in the coverage list at the end).

## Evidence and limits
- Every file and method named below comes from the Agent Brief and Evidence sections of the issue bodies, which are static code reading (confidence MEDIUM). Line numbers drift; implementers must re-read the code. Nothing was run for this plan.
- Existing tests: `src/test/java/com/lulan/shincolle/` holds `TestEnvironmentTest`, `EntityAIShipGuardingTest`, `CapaShipSavedValuesSectionTest`, `SoundResourceConsistencyTest`, `MetamorphSuppliesTest`, `PacketAuthorizationTest`. The pure-seam style (extract a predicate or function, test it without a Minecraft bootstrap) is the default; a seam that cannot be built that way goes back to the parent.
- A green `./gradlew.bat test` with unrelated tests, or a compile only, never verifies a batch. Each batch names the tests that must exist and fail before the fix (red) and pass after (green).

## Ordering rules applied
- P-7: #21 and #22 (and #23, same file) need a characterization test first. P-10: #15 migrates the old 2 values. P-15: #28 is a bug.
- #46 lands before #69 (same file, `ShipTank`). #18 lands before #17 (same method, `ShipSpawnEgg.onItemRightClick`).
- #55 lands before #53 (the radar sweep reuses `getEntityPlayerByUID`; #53 must not duplicate its fix).
- #70 (codec characterization tests) lands before #16, #26, #27 change codecs and handlers, so the tests describe pre-fix behavior.
- Perf issues (#52 to #57, #64 to #66) carry "nothing measured" warnings; they run after the measurement baseline (Batch 9) and each records before and after numbers or states that the gain is not measurable.
- #68 touches 19 files across all other batches, so it runs last to avoid conflicts. Temporary measurement counters from Batch 9 to 11 must be removed before merge (they must not leave `System.out` behind).
- Gradle rule (`.claude/rules/delegation.md`): never two Gradle or client processes on one checkout or dev world. "Parallel-safe" below means file-disjoint edits; test and build runs still need to be serialized, or each parallel worker needs a deliberate isolated worktree (it may miss uncommitted user work).

## Summary table

| Batch | Theme | Issues | Main files | Depends on | Parallel-safe with | Verification | Approval flags |
|---|---|---|---|---|---|---|---|
| 1 | Test and doc baseline | #70, #71 | `src/test/.../network/*CodecTest` (new), `docs/agents/*`, `CONTEXT.md` | none | 2, 3, 4, 6 | `./gradlew.bat test` (new codec tests); doc fact-check against `build.gradle` | New glossary terms need maintainer naming (`PROPOSED`) |
| 2 | Isolated small logic fixes | #20, #24, #46, #50, #28 | `TaskHelper`, `ShipPathNavigate`, `ShipTank`, `TileEntityHelper`, `EntityHelper.spawnBossShip` | none | 1, 3, 4, 6 | Named unit tests per seam; `test` + `build` | Fixes change gameplay numbers (#20, #50); no NBT/registry |
| 3 | Combat rolls and modifiers | #21, #22, #23 | `CombatHelper` (`BuffHelper` call site read-only) | none | 1, 2, 4, 5, 6 | `CombatHelperTest` characterization then fix; `test` + `build` | Balance change, approved by P-7 (#21, #22); #23 not under P-7 |
| 4 | Ship Spawn Egg | #18, #17 | `ShipSpawnEgg`, `ShipCalc` (read) | none (internal: #18 then #17) | 1, 2, 3, 6 | `ShipSpawnEggTest` seams; optional gametest spawn | No NBT change; failure-feedback only |
| 5 | Ship entity: death drop and ExpNext | #13, #19 | `BasicEntityShip`, `ConfigHandler.deathMaxTick` (read) | none | 1, 3, 4, 6 | Pure predicate/formula tests; optional headless gametest | First-10-ticks sub-case may need a rule decision |
| 6 | Crane config and load defaults | #15, #14 | `ConfigHandler`, `TileEntityCrane`, `CapaForgeEnergyStorage` (check) | none (internal: #15 then #14) | 1, 2, 3, 4 | Config-array and legacy-NBT load tests; `test` + `build` | Config migration approved (P-10); NBT keys unchanged (ADR-0002) |
| 7 | Server-side network hardening | #16, #27 | `PacketHelper`, `C2SGUIPackets`, `C2SInputPackets`, `CommandHelper` | 1 (#70 first) | 2, 3, 4, 5, 6 | `PacketHelperBoundsTest`, command page-cap test; `test` + `build` | Wire format must stay byte-identical (ADR-0004); cap value is a documented choice |
| 8 | Client-side handler robustness | #26, #25 | `S2CEntitySync`, `EventHandler`, client disconnect hook | 1, 7 | 2, 3, 4, 5, 6 | Handler no-throw tests; `ringAbility` restore test; `test` + `build` | #25 stop if intent is unclear; no wire change |
| 9 | Perf measurement baseline | (none, prerequisite) | Temporary counters only, not merged | 1 (#71 how-to) | 3, 4, 6 | Baseline numbers recorded in a report | Dev-only instrumentation, removed before merge |
| 10 | Server tick cost | #55, #53, #56 | `EntityHelper.getEntityPlayerByUID`, `ServerProxy.updateServerTick`, `BasicEntityShip.applyChunkLoader` | 9; 5 (BasicEntityShip); 2 (EntityHelper) | 3, 4, 6, 12 | Equivalence tests + before/after measurement; `test` + `build`; optional gametest | Packets byte-identical; nothing new persisted |
| 11 | AI polling cost | #52, #54, #57 | `EntityHelper.getEntitiesWithinAABB`, `TargetHelper`, `EntityAIShipRangeTarget`, `ServerProxy` (cache clear), `EntityAIShipGuarding`, `EntityAIShipPickItem` | 9, 10 | 3, 4, 6, 12 | Target-selection characterization test, cache-invalidation test, existing guard AI test; measurement | Target choice must not change; staggering is out of scope |
| 12 | Client render allocation | #64, #65, #66 | `RenderWaypoint`, `ParticleTextsCustom`, `ParticleTexts`, `RenderHelper`, `EventHandler` (HOTBAR branch) | 8 (EventHandler) | 2 to 7, 10, 11 | Extracted-condition/line-split tests; recorded in-game visual comparison | Output must be pixel-identical; needs a desktop client |
| 13 | Logging and dead-code cleanup | #68, #69 | 19 files for #68, `ShipTank` for #69, `LogHelper` | All other batches (#46 in Batch 2 before #69) | none (touches everything) | Grep for `System.out/err`/`printStackTrace` = 0; `test` + `build` | Logging only; `volumeTimekeep` question stays with the maintainer |

## Batch details

### Batch 1: Test and doc baseline (#70, #71)
Goal: write the codec characterization tests and the missing docs before any codec or handler change lands.
- Acceptance (rolled up):
  - #70: one round-trip test class per family (`S2CEntitySync`, `S2CGUIPackets`, `S2CReactPackets`, `S2CSpawnParticle`, `C2SGUIPackets`, `C2SInputPackets`), at least one test per buildable packet ID through `Unpooled.buffer()`; asymmetries (#44) are disabled tests citing the finding; IDs that cannot be built without a world are listed with the reason; no production edits; command and result recorded.
  - #71: gametest how-to (every command and path checked against `build.gradle:151-211` and `GameTestServerHooks`), glossary entries (Hime, Mount, Rensouhou, Equipment, Formation, Guard Point, Hostile Mob, Morph inventory, CMB-01, CMB-02) with `PROPOSED` where the English name is not agreed, one-page onboarding stating Java 25 / Cleanroom only (ADR-0001); no source, build or asset change.
- Edit ownership: #70 owns only new files under `src/test/java/com/lulan/shincolle/network/`. #71 owns new files under `docs/agents/` (or `docs/porting/`) and `CONTEXT.md`. Do not touch the untracked `docs/agents/claude-workflow.md`.
- Test plan: `./gradlew.bat test` for #70; #71 needs factual checks only (no Minecraft build required).
- Stop conditions (back to the parent): a packet class that cannot be instantiated without a Minecraft world (report the missing seam, do not edit production); any glossary term whose English name is not in the lang files or code; any read/write asymmetry found (log as a finding, do not fix).

### Batch 2: Isolated small logic fixes (#20, #24, #46, #50, #28)
Goal: five independent fixes in five different files, each with a pure seam.
- Acceptance:
  - #20: `levelLuckBonus(level, maxLevel)` gives 0.75 at 75/150, 1.5 at 150, about 0.01 at 1; no other luck term changes (`TaskHelper.java:858`).
  - #24: offset vector uses y for `yOffset`; push direction for dx in {-0.5, -0.1, 0, 0.1, 0.5} is {-, none, none, none, +}; manual path check recorded or noted not run (`ShipPathNavigate` around `:337-340`, `:468`).
  - #46: null trace result returns FAIL without throwing; block/fluid hits unchanged; no packet change (`ShipTank.onItemRightClick`).
  - #50: both item-fuel sites in `TileEntityHelper` (about `:46`, `:83`) apply `getFuelMagni()` as float; 1.0 identical, 0.5 gives half; no config change.
  - #28: `canSpawnByRing(checkRing, hasRing)` shared by `spawnBossShip` and `spawnMobShip`; default config unchanged.
- Edit ownership: `TaskHelper`, `ShipPathNavigate`, `ShipTank`, `TileEntityHelper`, `EntityHelper` (only the `spawnBossShip` cooldown block, plus the shared predicate), new test files. `ShipTank` stays free of #69 until Batch 13.
- Test plan: `TaskHelperLuckTest`, `ShipPathNavigateGeometryTest`, `ShipTankNullTraceTest`, `TileEntityHelperFuelTest`, `RingSpawnPredicateTest`, each failing first. Then `./gradlew.bat test` and `build`.
- Stop conditions: #20 historical intent differs from float scaling (8bca563 `TaskHelper`); #28 or #50 needing a config change; #46 needs more than the null guard.
- Note: #50 and #20 change gameplay numbers for non-default configs; report this in the verifier output.

### Batch 3: Combat rolls and modifiers (#21, #22, #23)
Goal: fix three arithmetic/logic bugs in `CombatHelper` after characterization tests (P-7).
- Acceptance:
  - #21: pure `rollOutcome(roll, miss, cri, dhit, thit, canMultiHit)`; characterization test first shows x3 unreachable; after the fix a roll between double and triple thresholds gives x3; other bands as before; `canMultiHit=false` still disables x2/x3; particle type 3 unchanged.
  - #22: characterization test first shows coef 1 returns the night-table value; after the fix coef 1 returns day, coef 0 night, 0.5 the midpoint, clamping and undefined types unchanged; verify the `BuffHelper.java:534` coefficient matches 0 = night / 1 = day and report if not.
  - #23: limit -1 leaves dodge unchanged; non-negative limit clamps as before; other `limitShipAttrs` uses listed in the PR, not fixed.
- Edit ownership: `CombatHelper` (single owner for all three), new `CombatHelperTest`. `Values` tables and `BuffHelper` are read-only.
- Test plan: commit-ready red/green order #21 and #22 characterization first, then fixes; `./gradlew.bat test` and `build`.
- Stop conditions: the test cannot load `Values`/`CombatHelper` without a Minecraft bootstrap (report the seam); the call-site light coefficient violates the contract (report, do not fix); another `limitShipAttrs` site has the same -1 bug (list it).
- Flag: #21 and #22 change combat balance; this is the approved P-7 outcome. Present the before/after table to the human.

### Batch 4: Ship Spawn Egg (#18, #17)
Goal: make the egg usage crash-free, then refund-safe.
- Acceptance:
  - #18: `costLevel(int[] attrs)` returns 0 for an empty or absent array and `attrs[0]/3` otherwise; manual `/give` check without `Attrs` recorded.
  - #17: egg and XP consumed only after a successful spawn; creative consumes nothing; ship class rolled from the unshrunk stack; forced failure leaves stack and XP unchanged; egg NBT layout unchanged.
- Edit ownership: `ShipSpawnEgg` (`onItemRightClick`, `getSpawnEntity`); `ShipCalc.rollShipType` read-only.
- Test plan: `ShipSpawnEggCostTest` for #18, then a consume-decision seam test for #17; `./gradlew.bat test` and `build`; optional gametest spawn if the harness supports it.
- Stop conditions: #17 requires touching the double `spawnEntity` / `readFromNBT` ordering (that is #36, `needs-info`, not ready); an `Attrs`-less egg should not be free (note for the maintainer). Land #18 first in the same session.

### Batch 5: Ship entity, death drop and ExpNext (#13, #19)
Goal: two `BasicEntityShip` fixes with pure seams.
- Acceptance:
  - #13: `shouldDropEgg(deathTime, deathMaxTick, canDrop)` uses a threshold comparison; with `Death_Time=0` and `CanDrop` exactly one egg; default 400 unchanged; `CanDrop` cleared on drop so never twice; `EntityHelper.saveShipDataToNBT` untouched.
  - #19: pure `expNext(level, expMod)`; `setExpNext`, level-up path and the initial array value agree; `ExpNext` right after `setShipLevel`; no NBT change.
- Edit ownership: `BasicEntityShip` only (`onDeathUpdate`, `setExpNext`, `addShipExp`, `setShipLevel`, initial `StateMinor`), new test files.
- Test plan: `ShipEggDropTest`, `ExpNextFormulaTest`; `./gradlew.bat test` and `build`; optional headless gametest for a sinking ship (needs the #71 how-to) because an entity-lifecycle change is not proven by a predicate test alone.
- Stop conditions: the first-10-ticks sub-case needs a rule decision (keep current behavior and report); `Death_Time` range change would be needed; #19's level-up formula `(ShipLevel + 2) * expMod` disagrees with `setExpNext` in a way that changes the exp curve (report, do not change the curve). Related, not in scope: #45 (hand-made ships vanished).

### Batch 6: Crane config and load defaults (#15, #14)
Goal: protect user configs and old saves for the Crane.
- Acceptance:
  - #15: 2-value `Tile_Crane` array becomes the first two slots of the 5-value array with defaults for the rest; 5-value unchanged; other wrong lengths reset to defaults; a log line for every migration or reset; key `Tile_Crane` unchanged.
  - #14: NBT without energy keys leaves EU and FE capacity at config defaults; with keys restores saved values (EU clamped to capacity as today); saved key set unchanged; `CapaForgeEnergyStorage.readFromNBT` checked for the same fault.
- Edit ownership: `ConfigHandler` (`getIntArrayFromConfig`, the `Tile_Crane` load), `TileEntityCrane.readFromNBT`, `CapaForgeEnergyStorage` if it has the fault, new tests.
- Test plan: `ConfigArrayMigrationTest`, `CraneLegacyNbtLoadTest`; `./gradlew.bat test` and `build`. Do #15 first so the #14 defaults come from the migrated config.
- Stop conditions: the legacy 2-value slot meaning cannot be confirmed from the 8bca563 config (back to the parent, may need `historical-researcher` or the issue re-labelled `needs-info`); any need for a new NBT key or changed config default.
- Flag: ADR-0002 applies (load behavior only, no new keys); P-10 already approves the migration.

### Batch 7: Server-side network hardening (#16, #27)
Goal: bound decoder allocations and fix the rename/list issues in server handlers.
- Acceptance:
  - #16: length validated (non-negative, at most `readableBytes()/elementSize`, per-call maximum) before allocation in `PacketHelper.readIntArray`, `readByteArray` and the string/float-array readers; every `fromBytes` site that reads a length covered or listed as exempt; valid packets byte-identical.
  - #27: dead `s.sendSyncRequest(1)` replaced by the server-to-client sync path (found by reading how other name changes sync) or documented as unnecessary; "show all" in `CommandHelper.processSendShipList` capped or paged at a documented limit; page > 0 unchanged.
- Edit ownership: `PacketHelper`, `C2SGUIPackets` (unit-name case and length call sites), `C2SInputPackets` (length call sites), `CommandHelper`, plus any other packet class with a packet-supplied length. If a call site is in `S2CEntitySync`, hand it to Batch 8 or run Batch 8 afterward.
- Test plan: `PacketHelperBoundsTest` (oversize, negative, valid round trip), page-cap test; Batch 1 codec tests must stay green; `./gradlew.bat test` and `build`; a dedicated-server rename check recorded manually or by the gametest server, or noted not run.
- Stop conditions: any wire-format change; the rename sync needs a new packet; the cap value is a gameplay choice. Authorization problems seen on the way go to #29 (`ready-for-human`), not this batch.

### Batch 8: Client-side handler robustness (#26, #25)
Goal: client handlers never throw on mismatched packets, and one string comparison bug.
- Acceptance:
  - #26: world and entity null checks plus `instanceof` before every cast in the S2C handlers (the PR lists each site fixed), mismatches ignored with a debug log; local `ringAbility` saved before the first server sync and restored on disconnect/world unload; no wire change.
  - #25: `.equals` used for the attribute name only after the intent of the block is established; attribute named `generic.maxHealth` gets `Base` = 20, other names untouched; the PR states the scenario (phantom ship HP) and whether it was exercised.
- Edit ownership: `S2CEntitySync`, `EventHandler` (the attribute fix-up around `:1128` and the disconnect hook), new tests. Batch 12 also edits `EventHandler` (HOTBAR branch), so serialize.
- Test plan: handler no-throw test with unknown entity ID and wrong type; sync-then-disconnect `ringAbility` test; attribute-name test; `./gradlew.bat test` and `build`.
- Stop conditions: #25 intent cannot be established or enabling the block could rewrite saved data (report, leave disabled; see "Not ready"); #26 handler testing needs a client world that cannot be faked (report the seam).

### Batch 9: Performance measurement baseline (no issue)
Goal: produce the before numbers that #52, #53, #55, #56 (and optionally #57, #64 to #66) require, once, on a fixed fleet.
- Acceptance: written baseline (spark profile or temporary nanoTime counters) for `EntityAIShipRangeTarget.shouldExecute` (30 and 60 idle ships, 200 mobs/items), the radar block (0/100/500/2000 registered ships), `getEntityPlayerByUID` calls per second (30/60 ships, 1/10 players), `applyChunkLoader` runs and unchanged-chunk fraction (30 chunk-loader ships). Same world and same save for the later "after" runs. Counters are not merged.
- Edit ownership: temporary patch only, discarded; output goes to a report (`/porting-report`), not into production code.
- Test plan: run on the dedicated/gametest server only after Batch 1 (#71 how-to) exists; one Gradle/server process at a time.
- Stop conditions: no repeatable fleet fixture; baseline shows no measurable cost for an item (report this to the human so the item can be dropped or re-labelled; do not decide it here).
- Note: this batch carries no issue, so it is not counted in the issue coverage list. It is a prerequisite for Batches 10 and 11.

### Batch 10: Server tick cost (#55, #53, #56)
Goal: remove linear scans from per-tick and periodic server paths without behavior change.
- Acceptance:
  - #55: `getEntityPlayerByUID(int)` keeps its signature and null contract, returns the same online player in every dimension and null for offline or `uid <= 0`; no stale player after logout or dimension change; before/after measurement.
  - #53: same per-player entity ID lists and `SyncGUI_ShipList` payload for a fixed set of players and ships; offline players and dead/unloaded ships produce no work; `Radar_Update` interval and "any player online" condition unchanged; packet skipping only if verified client-equivalent (otherwise keep sending); before/after measurement.
  - #56: early-out in `applyChunkLoader` keyed on chunk x/z, `chunkloaderMode` and ticket identity; changing mode or losing the ticket re-applies; new fields transient, nothing persisted.
- Edit ownership: `EntityHelper.getEntityPlayerByUID` (not other methods), `ServerProxy.updateServerTick` radar block, `BasicEntityShip.updateChunkLoader/applyChunkLoader`. Batch 2 (#28 in `EntityHelper`) and Batch 5 (`BasicEntityShip`) must be merged first or use the same session.
- Test plan: extracted lookup/equivalence tests; position-to-chunk-set test; recorded before/after numbers from Batch 9; `./gradlew.bat test` and `build`; headless gametest for radar/chunk behavior if feasible.
- Stop conditions: measurement shows no gain; packet skipping cannot be proven client-identical; any need to prune registered maps (saved data, out of scope).

### Batch 11: AI polling cost (#52, #54, #57)
Goal: cheaper AI scans with identical target choice.
- Acceptance:
  - #52: typed world lookup with the selector as predicate in the shared entity lookup helper; per-class cache of the unattackable-class decision with the same key semantics (hash of the simple class name); the cache is cleared when the server proxy edits the unattackable list (test); same targets, same scan order, same 8-tick phase and grow factors; before/after measurement.
  - #54: the name loop and concatenation in `EntityAIShipGuarding.findTarget` run only under `ConfigHandler.debugMode`, message text unchanged; existing guard AI test still passes.
  - #57: has-space result cached for a few ticks and invalidated on inventory change (lifetime documented, at most 16 ticks); sort only for two or more items; same item picked.
- Edit ownership: `EntityHelper.getEntitiesWithinAABB`, `TargetHelper`, `EntityAIShipRangeTarget`, `ServerProxy` (cache invalidation only), `EntityAIShipGuarding`, `EntityAIShipPickItem`. Serialize after Batch 10 (`EntityHelper`, `ServerProxy`) and Batch 2.
- Test plan: fixed-arrangement target-selection characterization test written first, cache invalidation test, `EntityAIShipGuardingTest`; `./gradlew.bat test` and `build`.
- Stop conditions: the unattackable list can be changed by a path other than the server proxy; selection order differs even by one entity in the characterization test; #57 shows no measurable gain (report; low priority).

### Batch 12: Client render allocation (#64, #65, #66)
Goal: remove per-frame allocations in three render paths with pixel-identical output.
- Acceptance:
  - #64: waypoint `Item` cached lazily after registration, early return on null `Minecraft.player`, no `new ItemStack` per frame, render conditions unchanged.
  - #65: text split once at particle construction, no per-frame wrapping, positions/colors/lifetime/background unchanged.
  - #66: early-out before the four array allocations, event resolution used instead of `new ScaledResolution`, max cooldown delays computed once per frame, cooldown text equal to `String.format('%.1f')` output, GUI scaling via `posHUD` still works.
- Edit ownership: `RenderWaypoint`, `ParticleTextsCustom` (and `ParticleTexts` only if obviously safe), `RenderHelper.drawPlayerSkillIcon`, the HOTBAR branch of `EventHandler`.
- Test plan: tests for the extracted conditions, the line split with a fake width and the cooldown formatter; a recorded in-game visual comparison (needs a desktop client) or marked not run; `./gradlew.bat test` and `build`.
- Stop conditions: no identical text-formatting equivalent for `String.format` rounding cases (report); visual comparison impossible (record "not verified in-game").
- Priority: lowest for gameplay; do not start before Batch 8 has released `EventHandler`.

### Batch 13: Logging and dead-code cleanup (#68, #69)
Goal: last, mechanical, conflict-prone cleanup.
- Acceptance:
  - #68: 46 uses of `System.out`, `System.err`, `printStackTrace` in 19 files replaced by `LogHelper` calls with message text kept; new `LogHelper` overload that takes a message and a `Throwable`; a grep over `src/main` finds none; commit contains logging changes only.
  - #69: `ShipTank.getBlockInFrontOfPlayer` removed after #46 merged; text search finds nothing in `src`.
- Edit ownership: all files listed in #68 (`CapaTeitoku`, `EntityHelper`, `BasicTileInventory`, `PacketHelper`, `EventHandler`, `BasicShipConfig`, `RenderHelper`, `TileEntityCrane`, `ServerProxy`, `S2CReactPackets`, `C2SInputPackets`, `ShipSpawnEgg`, `ModBlocks`, `LargeRecipes`, `ShipCmdUpdateOwnerUID`, `RenderBasic`, `GuiFormation`, `GuiDesk`, `CapaShipInventory`), `LogHelper`, `ShipTank`.
- Test plan: grep for the three patterns returns no match; `./gradlew.bat test` and `build`.
- Stop conditions: a use that must stay on the console (list it as a commented exception); a bug seen in the surrounding code (log it as a finding, do not fix); `volumeTimekeep` stays untouched (config key; maintainer question on #69).

## Dependency and conflict matrix
"C" = shared production file, must serialize (or merge in order); "D" = depends on; blank = disjoint.

| | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 | 13 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | | | | | | | | | | | | | |
| 2 | | | | | | | | | | C (`EntityHelper`) | C (`EntityHelper`) | | C |
| 3 | | | | | | | | | | | | | |
| 4 | | | | | | | | | | | | | C (`ShipSpawnEgg`) |
| 5 | | | | | | | | | | C (`BasicEntityShip`) | | | |
| 6 | | | | | | | | | | | | | C (`TileEntityCrane`) |
| 7 | D | | | | | | | C? (`S2CEntitySync` if a length site is there) | | | | | C |
| 8 | D | | | | | | C? | | | | | C (`EventHandler`) | C |
| 9 | D (#71) | | | | | | | | | | | | |
| 10 | | C | | | C | | | | D | | C (`EntityHelper`, `ServerProxy`) | | C |
| 11 | | C | | | | | | | D | C | | | C |
| 12 | | | | | | | | C | | | | | C |
| 13 | all | all | all | all | all | all | all | all | | all | all | all | |

Conflict hot spots by file:
- `EntityHelper`: Batch 2 (#28), 10 (#55), 11 (#52), 13 (#68). Serialize in that order.
- `BasicEntityShip`: Batch 5 (#13, #19), 10 (#56). Batch 5 first.
- `ServerProxy`: Batch 10 (#53), 11 (#52 cache clear), 13 (#68).
- `EventHandler`: Batch 8 (#25, #26), 12 (#66), 13 (#68). Batch 8 first.
- `ShipTank`: Batch 2 (#46) then Batch 13 (#69).
- `ShipSpawnEgg`: Batch 4 (#18 then #17), then Batch 13 (#68).
- `ConfigHandler`: Batch 6 only (and read-only references in 3 and 5).
- `PacketHelper`, `C2SInputPackets`: Batch 7 then Batch 13.
- `CombatHelper`: Batch 3 only. `TaskHelper`, `ShipPathNavigate`, `TileEntityHelper`: Batch 2 only.

## Recommended execution order

| Step | Batches | Worker | Verifier | Note |
|---|---|---|---|---|
| 1 | 1 (#70, #71) | `sonnet-implementer` (tests), `sonnet-implementer` or `porting-doc-writer` for #71 prose | `regression-verifier` | Safe, no production edits. Must be done before Batches 7 and 9. |
| 2 | 2, 3, 4, 6 | one `sonnet-implementer` per batch; file-disjoint | one `regression-verifier` per batch; Gradle runs serialized | Quick wins, then characterization-first fixes (3). Do not start two Gradle processes in this checkout. |
| 3 | 5, then 7 | `sonnet-implementer` | `regression-verifier`; optional headless gametest for 5 | Entity behavior and wire hardening. 7 after Batch 1 codec tests are green. |
| 4 | 8 | `sonnet-implementer` | `regression-verifier` | After 7; stop at #25 intent. |
| 5 | 9 | `sonnet-implementer` (instrumentation, discarded) | `regression-verifier` checks the report | Needs Batch 1 gametest how-to. |
| 6 | 10, then 11 | `sonnet-implementer` | `regression-verifier` plus recorded before/after numbers | After Batch 2 and 5 are merged. |
| 7 | 12 | `sonnet-implementer` | `regression-verifier`; human visual check | After Batch 8. |
| 8 | 13 | `sonnet-implementer` | `regression-verifier` | Last. Rebase over everything merged. |

After every batch the parent decides whether to continue; this plan does not advance any phase. Every batch ends at a human review of the diff and verification output (no commit or push by the worker).

## Not ready / needs human
No label is changed here. These are recommendations for the maintainer.
- #25 (Batch 8): the intent of the ghost-HP block is unknown (MOD-02). The brief says to report instead of enabling it. Candidate for `needs-info` or `ready-for-human`. The batch has a stop condition; the rest of Batch 8 (#26) is unaffected.
- #15 (Batch 6): the legacy 2-value slot meaning is "not verified" in the issue. A historical lookup (8bca563 config) is needed before the migration mapping is written. Candidate for `needs-info` until confirmed; research can run first.
- #13 (Batch 5): the first-10-ticks sub-case is a rule question. The Death_Time=0 half is ready; the other half is a decision. Related unresolved Unknown: #45.
- #17 (Batch 4): touches the same method as #36 (`needs-info`, double `spawnEntity`); keep the scope to consume ordering and do not touch spawn order.
- #21 and #22 (Batch 3): ready (decided in P-7), but they change combat balance; the human should see the before/after table.
- #71 (Batch 1): glossary names that are not in lang files or code stay `PROPOSED` and need maintainer answers; the doc parts are ready.
- Performance issues #52, #53, #55, #56 (and #57, #64, #65, #66 at low priority): every claim is an unmeasured hypothesis. They are workable only after Batch 9. If the baseline shows no measurable gain, they should be reconsidered (close or re-label) by the human. #54 is a safe cleanup that needs no measurement.
- #69: ready, but the `volumeTimekeep` question (leave, deprecate, or remove with migration; config file change) is an open maintainer decision that is outside the brief.

## Coverage check (31 issues, each exactly once)
Batch 1: #70, #71. Batch 2: #20, #24, #28, #46, #50. Batch 3: #21, #22, #23. Batch 4: #17, #18. Batch 5: #13, #19. Batch 6: #14, #15. Batch 7: #16, #27. Batch 8: #25, #26. Batch 9: none. Batch 10: #53, #55, #56. Batch 11: #52, #54, #57. Batch 12: #64, #65, #66. Batch 13: #68, #69.

Count: 2 + 5 + 3 + 2 + 2 + 2 + 2 + 2 + 3 + 3 + 3 + 2 = 31, matching the 31 open `ready-for-agent` issues returned by the query: #13 to #28, #46, #50, #52 to #57, #64 to #66, #68 to #71.
