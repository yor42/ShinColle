# Optimization and Code Health Review: Filed Issues #52-#74

- Date: 2026-10-14
- Phase: 3 (Legacy Stabilization)
- Author: Claude Code (sonnet-implementer, issue filing)
- Related issue/ADR: ADR-0001 (Java 25, Cleanroom only), ADR-0002 (save compatibility), ADR-0004 (server-authoritative packets); stabilization log `2026-10-14-stabilization-findings.md`; decisions `../decisions/README.md` (P-3, P-4, P-13)

> **STATIC ANALYSIS ONLY. Nothing in this document was measured, profiled or run.** Every performance statement is a hypothesis from code reading (confidence at most MEDIUM). No finding here is CONFIRMED_WORKING or a confirmed hotspot. The recommended first step for every performance item is to measure (spark profiler, a temporary timing counter, or a gametest scenario), then optimize.

## Scope
Static review of the 1.12.2 legacy track at HEAD `4f74ad0` plus the uncommitted working tree: server tick cost, client rendering cost, and code health. 23 findings were filed as GitHub issues on 2026-10-14 (#52 to #74; creating issues was approved by the user). Each issue has one category label (`enhancement`, or `bug` for S-F1) and one state label. The `enhancement` label already existed in the repository (description "New feature or request"); no label was created or edited.

## Evidence rules
- Every citation was re-read against the current tree on 2026-10-14. Line numbers drift as fixes land.
- Corrections made while re-verifying (the first-draft numbers were wrong or imprecise):
  - Path cache radius: `range + 8` for position paths (`ShipPathNavigate.java:128`) but `range + 16` for entity paths (`:152`), not `+8` for both. Re-path timers are at `EntityAIShipAttackOnCollide.java:78` and `EntityAIShipRangeAttack.java:158` (draft said `:84`).
  - `RenderWaypoint`: `checkOP` is evaluated after the held-item early-out, not before it; only the `ItemStack` allocation and the missing null check apply (C-3).
  - HUD (C-6): only the four small arrays are allocated for every player on every HOTBAR event; `ScaledResolution`, `String.format` and five `getAttackDelay` calls occur only while riding a ship mount, carrying a ship, or morphed.
  - Counts: 462 Java files in `src/main` (draft 461); 58 `TODO`/`FIXME` in 32 files (draft 59/33); 24 `*Helper` classes in `utility/` with 347 `public static` members including fields (draft 26/343); 61 `@SideOnly` annotations in 33 files outside `client/` (88 in 59 files in total; draft 93/63). `ConfigHandler` has 86 mutable public static fields (confirmed), `EntityHelper` 2181 lines (draft 2182), `BasicEntityShip` 5007 lines (draft 5008), `ParticleHelper` has about 130 `spawnParticle` call sites (draft ~150).
  - `EntityAIShipPickItem`: the sort at `:176` runs whenever the list is non-empty (not only for 2+ items); the scan runs every 16 ticks.
- No finding was skipped as stale: all cited code paths still exist.

## Server findings (path prefix `src/main/java/com/lulan/shincolle/`)
| ID | Issue | State | Impact (hypothesis) | Confidence | Measure first | Safe vs needs decision |
|---|---|---|---|---|---|---|
| S-F2 target AI scans (`ai/EntityAIShipRangeTarget.java:61-130`, `utility/EntityHelper.java:1195-1208`, `utility/TargetHelper.java:262-340,44-53`) | #52 | ready-for-agent, enhancement | Medium with many idle ships | MEDIUM (path), LOW (size of gain) | spark share of `shouldExecute`; 30/60 idle ships, 200 entities | Safe: typed lookup, per-class cache of the unattackable check. Needs decision: staggering the 8-tick phase (out of scope) |
| S-F4 radar sweep (`proxy/ServerProxy.java:685-776`) | #53 | ready-for-agent, enhancement | Grows with registered ships/players | MEDIUM | timing counter around the sweep with 0/100/500/2000 registered ships | Safe if packet content stays identical |
| S-F6 debug string (`ai/EntityAIShipGuarding.java:340-352`) | #54 | ready-for-agent, enhancement | Low | HIGH (wasted work), LOW (measurable) | optional | Safe, no behavior change |
| S-F7 owner lookup (`utility/EntityHelper.java:383-410` and callers) | #55 | ready-for-agent, enhancement | Medium with many ships | MEDIUM | call counter and ms/s with 1 and 10 players | Safe, same return contract |
| S-F11 chunk loader (`entity/BasicEntityShip.java:4382-4440`, `utility/BlockHelper.java:512`) | #56 | ready-for-agent, enhancement | Low to medium | MEDIUM | fraction of runs with unchanged chunk | Safe (early-out when chunk, mode, ticket unchanged). `TaskHelper.onUpdatePumping` (`:1854-1856`) is a profiling candidate only, not read |
| S-F8 pick-item AI (`ai/EntityAIShipPickItem.java:58-72,165-176`) | #57 | ready-for-agent, enhancement (low) | Low | LOW | optional | Safe if cache lifetime stays within the 16-tick scan |
| S-F1 unbounded ship cache (`entity/BasicEntityShip.java:3012-3032`, `proxy/ServerProxy.java:423-427,610-660`, `server/ShinWorldData.java:148-166`) | #58 | needs-info, bug | Potentially large on long-lived saves | MEDIUM (no removal path found), LOW (size) | world-data file size vs ship count, heap, autosave spike | Safe half: snapshot only when the ship changed. Needs decision and ADR-0002 approval: pruning entries (relates to #31) |
| S-F3 attribute resend (`entity/BasicEntityShip.java:1889-1913,1949,1105-1109`) | #59 | needs-info, enhancement | About 1.4 packets/ship/s | MEDIUM | packet and byte counters; spark | Needs decision (cadence, ADR-0004) |
| S-F5 depth check (`utility/EntityHelper.java:148-187`) | #60 | needs-info, enhancement | Up to 64 lookups/tick/ship in liquid | MEDIUM | timing counter, 60 ships afloat | Inline loop is safe; caching changes float timing (decision) |
| S-F9 path requests (`ai/path/ShipPathNavigate.java:118-157`, `ShipPathFinder.java:202`) | #61 | needs-info, enhancement | Bursty during chases | LOW-MEDIUM | requests/s and ns/request | Needs decision (radius or frequency changes behavior) |
| S-F12 emotion/rider sync (`entity/BasicEntityShip.java:1349-1355,1480-1490,1937-1944`) | #62 | needs-info, enhancement | Unknown | LOW | per-type packet histogram | Needs decision (ADR-0004) |

### Server, doc only (no issue)
- S-F10 (low): `utility/BuffHelper.java:555-610`, `applyBuffOnHeal` serializes each nearby `EntityAreaEffectCloud` to NBT (`writeToNBT`) just to read potions. Impact low; revisit only if a profile points to it.

### Server not covered
`EventHandler` per-tick hooks; `CapaTeitoku` and inventory serialization; mounts, airplanes and projectiles; `TargetHelper` `:330-450` (the other selectors); smaller tile entities; world generation; `BasicEntityShipHostile` ticking; the remaining packets; thread-safety of the `ServerProxy` `HashMap`s against netty threads (see #35).

## Client findings
Client summary: no large hotspot was found by reading. Packet handlers defer through `addScheduledTask`; GUI strings resolve once; textures are 197 PNGs (1.9 MB) and sounds 253 oggs (6 MB); models are static and shared per class.

| ID | Issue | State | Impact (hypothesis) | Confidence | Measure first | Safe vs needs decision |
|---|---|---|---|---|---|---|
| C-1 particle fan-out (`utility/ParticleHelper.java:120+`; `entity/BasicEntityShip.java:1726,1751,4799`) | #63 | needs-info, enhancement | Possible FPS drops in large fights | LOW-MEDIUM | particles per type per second, FPS, 10/30/60 ship fight | Needs decision (visual change) |
| C-3 waypoint renderer (`client/render/block/RenderWaypoint.java:33-48`) | #64 | ready-for-agent, enhancement | Low | MEDIUM | optional allocation profile | Safe, render conditions unchanged |
| C-5 text particles (`client/particle/ParticleTextsCustom.java:77-112`, `ParticleTexts.java:43-76`) | #65 | ready-for-agent, enhancement | Low | MEDIUM | text particles alive vs frame time | Safe, pixel-identical output |
| C-6 HUD (`handler/EventHandler.java:989`, `utility/RenderHelper.java:144-250`) | #66 | ready-for-agent, enhancement | Low | MEDIUM | optional allocation profile | Safe, pixel-identical output |
| C-fragile client-only references (`entity/BasicEntityShip.java:23`; `network/S2CSpawnParticle.java:176-215`) | #67 | needs-info, enhancement | Robustness, not speed | LOW-MEDIUM | dedicated-server load of every ship class | Audit only until approved |

### Client, doc only (no issue)
- C-2 (low): `client/render/RenderBasic.java:215-293` `setBrightness` issues about 40 `glTexEnv` calls, only when a ship is hurt, dying or tinted; the shadow-size switch runs each frame (`:42-80`). Negligible by reading.
- C-4 (low): `BasicEntityShip.isGlowing` (`:4958-4969`) calls `TeamHelper.checkSameOwner(this, ClientProxy.getClientPlayer())` for invisible ships each query; memoize only if a profile shows it.

### Client not covered
About 80 `Model*` classes; `ShipModelBaseAdv` emotion code; large GUI draw loops (`GuiShipInventory`, `GuiDesk`, `GuiBook`, `GuiFormation`); `RenderTileEntityItem` and the Large Shipyard TESR; `sounds.json` and lang reload cost; `ClientProxy` caches; `Particle91Type`, `ParticleChi`, `ParticleSpray` and `ParticleLaser` render cost; shader compatibility (`GL_COMBINE` in `setBrightness`); the Metamorph `NoClassDefFoundError` path (#43).

## Code health findings
| ID | Issue | State | Notes |
|---|---|---|---|
| H-1 console output | #68 | ready-for-agent, enhancement | 46 uses of `System.out`/`System.err`/`printStackTrace` in 19 files (`CapaTeitoku` 20). Add a `Throwable` overload to `utility/LogHelper.java` (comments there are Traditional Chinese: leave untranslated). Messages unchanged |
| H-2 dead code | #69 | ready-for-agent, enhancement | `ShipTank.getBlockInFrontOfPlayer` (`item/ShipTank.java:63`) has no callers. Land after #46 (same file). Open question: `volumeTimekeep` (`handler/ConfigHandler.java:145,300`) is unreferenced but removing it changes the config file; NOT to be removed without approval |
| H-3 packet round-trip tests | #70 | ready-for-agent, enhancement | Tests only; bugs found go to separate findings (relates to #16 NET-03, #44 NET-08) |
| H-4 docs | #71 | ready-for-agent, enhancement | Gametest how-to (`-Pgametest`, `gametestServerArgs`, `-Pgametest_spawnoff`, `-Dshincolle.gametest.shipload=true`; results paths `run/client/gametest/results.txt`, `run/server-gametest/gametest-shipload-results.txt`; `build.gradle:151-211`), missing `CONTEXT.md` terms (Hime, Mount, Rensouhou, Equipment, Formation, Guard Point, Hostile Mob, Morph inventory, CMB-01/02 codes; unagreed ones `PROPOSED`), onboarding page |
| H-5 CI | #72 | ready-for-human, enhancement | `.github/workflows/gradle.yml`: both jobs `permissions: write-all`; the `pull_request` trigger starts no job (pre-release only on push to `mc-1.12.2`, release only on tags); old `checkout@v3`, `setup-java@v3`, `gradle-build-action@v2`, `upload-artifact@master`. Build/CI change needs approval |
| H-6 static analysis/format | #73 | needs-info, enhancement | Advisory mode only (errorprone or spotbugs, Spotless `ratchetFrom`, `-Xlint:all` baseline). Plugin compatibility with Unimined-remapped sources unverified. Build change needs approval |
| H-7 structure options | #74 | needs-info, enhancement | Umbrella: minimum Minecraft-independent seams (`CombatHelper` roll, `ShipCalc`/`EquipCalc` plain inputs with injected `Random`, `ConfigHandler` snapshot), packet registry (ADR-0004), splitting `BasicEntityShip`/`EntityHelper`, ship-class deduplication (about 40 classes plus Mob variants; unverified). `crafting/EquipCalc.java:3-5` imports `ContainerShipInventory`, `BasicEntityShip`, `CapaTeitoku` (possible crafting to GUI to entity cycle; unverified; suggest a read-only `jdeps` or ArchUnit check). P-3/P-4 still open |

### Code health, doc only (H-8, no issue)
- `reference/Values.java` uses double-brace `HashMap` initializers (15 `{{` blocks); candidates for `Map.of` or builders.
- `@SideOnly` outside `client/`: 61 annotations in 33 files, not yet filtered into real leaks (see #67).
- Each class has its own static `Random`.
- Chinese comments: keep or translate is a human judgment call (no change without approval).
- Totals: 462 files, about 148k lines; 58 `TODO`/`FIXME` in 32 files; no tests for `CombatHelper`, `ShipCalc`, `EquipCalc`, `TargetHelper`, the packet codecs or `ConfigHandler`.

## Recommended order
1. Docs and glossary (#71); no risk.
2. Measure: install spark on a dedicated test server and record baselines (or add temporary timing counters) with fixed fleets of 10, 30 and 60 ships, using the existing gametest ship-load scenario where possible. Each perf issue states its own plan. Do not optimize before the numbers exist.
3. Characterization tests: packet codecs (#70), combat (#21, #22), NBT legacy fixture (ADR-0002 / P-13).
4. Zero-behavior cleanups: #54, #55, #56, #52, #53, #57, #64, #65, #66, #68, #69 (after #46).
5. Decisions: #58 (with #31), #59, #60, #61, #62, #63, #67.
6. Build and CI (#72, #73), then structure options (#74) in Phase 5.

## Compatibility risks
- Anything that changes packet cadence or content (S-F3, S-F12, S-F4 if the payload changes) is an ADR-0004 question; the client may depend on periodic resends.
- Pruning the ship cache (S-F1) changes saved data and `/shipget` recovery (ADR-0002, #31).
- Caching in S-F5 changes float timing that #6 deliberately restored.
- Particle changes (C-1) are visible to players.
- Target-selection caches (S-F2) must reproduce the existing simple-class-name hash semantics, including collisions.
- Edits to `ShipTank.java` (#46, #69) must be sequenced.

## Assumptions
- Costs scale with ship counts as the code suggests; nothing was measured.
- Registered ship/player maps are much larger than the loaded set on long-lived saves (#53, #58).
- The packet receivers tolerate the current periodic resends and may depend on them (#59, #62).

## Unknowns
- Actual tick or frame cost of any item above.
- Whether `TaskHelper.onUpdatePumping` or other unread code is the real server hotspot.
- Whether the Unimined/Cleanroom toolchain accepts the analysis plugins in H-6.
- Whether `EquipCalc` is part of a real dependency cycle.
