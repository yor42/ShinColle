# Gap Analysis: Ship Entity Inheritance and Lifecycle (ENT-)

- Date: 2026-10-13
- Phase: 2
- Author: Claude Code (legacy-code-analyst subagent; ENT-02 and ENT-08 spot-verified by the main session)
- Related issue/ADR: backlog `2026-10-13-gap-backlog.md`

## Scope
The entity class trees (`BasicEntityShip`, `BasicEntityShipSmall`, `BasicEntityShipCV`, `BasicEntityShipHostile`, `BasicEntityMount(+Large)`, `BasicEntityAirplane`, `BasicEntitySummon`, `IShip*`), registration (`init/ModEntity.java`), Abyssal spawning, the Ship Spawn Egg birth path, death and egg drop, despawn, the UID cache and revive, mounts, summons, chunk loading, and dimension change. Shipyard tile internals were only partly traced (ENT-05). Static analysis: a whitespace- and brace-normalized diff against `8bca563`. Nothing was run.

Result of the normalized diff: apart from 1.12 API renames, the lifecycle logic is unchanged from 8bca563. Registration is identical: the 87 name/class/ID-order entries match (`init/ModEntity.java:46-146`).

## Evidence
1. Main-session verification: `BasicEntityShip.java:1429-1431` uses `getDistanceSq(player) < 64F`, and `:1489-1491` uses `getDistanceSq(player) <= 64F`. 8bca563 `BasicEntityShip.java:1594,1660` used `getDistanceToEntity(player) < 64F` / `<= 64F`. The squared form came in with `7b71860`.
2. Main-session verification: `onDeathUpdate` does `++this.deathTime` and then `if (this.deathTime == ConfigHandler.deathMaxTick)` (`BasicEntityShip.java:4787-4800`). `Death_Time` allows a minimum of 0 (`ConfigHandler.java:184`).
3. `createEntity` uses `EntityEntryBuilder` with `ResourceLocation(MOD_ID, name)`, `.name("shincolle."+name)`, and tracker `(64,1,true)` (`init/ModEntity.java:169-181`). 8bca563 used `registerModEntity` with the same list.
4. No `DataFixer`, `registerWalker` or `FixesEntity` exists anywhere in the mod (grep).

## Confirmed findings

| ID | Feature | Status | Conf. | Historical (8bca563) | Current (HEAD) | Desired (proposal) | Evidence |
|---|---|---|---|---|---|---|---|
| ENT-01 | Class hierarchy and registration | No status; no defect (not CONFIRMED_WORKING) | MEDIUM | Same 87 entries via `registerModEntity` | Separate trees for friendly and hostile ships (shared interfaces only); `EntityEntryBuilder`; no registry spawn eggs | Freeze names and numeric IDs; document a registry table | Ev. 3 |
| ENT-02 | Death → Ship Spawn Egg drop | CONFIRMED_BUG (edge case, inherited) | HIGH | Same | With `Death_Time=0`, the `==` test never matches, so the ship vanishes with no egg (**data loss**). Ships killed in their first 10 ticks drop nothing unless `CanDrop` was restored from NBT (`:1865-1866`). | Drop on `>=` with the `CanDrop` guard, or clamp the config minimum to 1 (config change needs approval) | Ev. 2 (verified) |
| ENT-03 | Ship Spawn Egg use | CONFIRMED_BUG (consumed before spawn) / UNKNOWN_INTENT (material roll) | HIGH / LOW | Same order | XP and the egg are spent (`ShipSpawnEgg.java:437-454`) before `getSpawnEntity`, which can return null (`:245-263`). No refund. `rollShipType` reads the tag after the shrink (`ShipCalc.java:95-103`). Double `spawnEntity` (`EntityHelper.java:1676`; `ShipSpawnEgg.java:462,472`). | Spawn first, then consume; read the NBT before shrinking | static |
| ENT-04 | Abyssal spawning (player-tick model) | UNKNOWN_INTENT (ring gate) | MEDIUM | Unchanged; natural biome spawn already commented out | `spawnMobShip` and `spawnBossShip` (`EntityHelper.java:1936-2160`) from the player tick (`EventHandler.java:541-557`). The boss cooldown requires `hasRing()` even when `Check_Ring=false`. | Keep the model; decide the ring gate | static |
| ENT-05 | Shipyard → egg | UNKNOWN_INTENT (not fully traced) | LOW | API-only diff | Recipes produce egg meta 0/1 with material NBT; the class is rolled at use time | Separate shipyard pass | static |
| ENT-06 | Despawn rules | No status; no defect | MEDIUM | Same | Friendly ships never despawn. Hostile ships despawn by config ticks (-1 = never). Tagged eggs never expire. Eggs cannot use portals (`BasicEntityItem.java:95-97`). | Record as the contract | diff |
| ENT-07 | UID cache, dupe resolution, `/shipget` revive | UNKNOWN_INTENT (revive position) | MEDIUM | Same | `createShipEntity` reads NBT after `setPosition`, so a revived ship appears at its cached position, not at the player (`EntityHelper.java:1664-1700`; `ShipCmdGetShip.java:140-260`). `checkShipIsDupe` acts after 200 ticks (`ServerProxy.java:577-605`). | Maintainer decision | static |
| ENT-08 | Owner sync radius: **64 blocks → 8 blocks** | CONFIRMED_BUG (port regression, 7b71860) | HIGH | `getDistanceToEntity < 64` (linear) | `getDistanceSq < 64` (squared, so 8 blocks) for both the GUI and the entity sync | `getDistanceSq(player) < 4096D` | Ev. 1 (verified) |
| ENT-09 | Mount lifecycle | No status; no defect (one TODO) | MEDIUM | Same | Mounts are transient (no NBT); they are killed on reload when `host==null` and re-summoned by the ship (`BasicEntityMount.java:457-463`) | Keep; document that mounts never persist | diff |
| ENT-10 | Airplane and summon lifetime | No status; no defect | MEDIUM | Same | Dies with the host. Summon life is 1200 ticks. Only `scaleLV` persists. | Record as the contract | diff |
| ENT-11 | Chunk loading and dimension change | UNKNOWN_INTENT | LOW | Same | Tickets are released on world load (`ChunkLoaderHandler.java:18-26`). No ship `changeDimension` override. `/shipget` refuses cross-dimension revive. Possible NPE if `chunks != null` while `chunkTicket == null` (`:4355-4362`; no path found that reaches it). | Define portal behavior | static |
| ENT-12 | Saved entity IDs and old-world migration | UNKNOWN_INTENT | MEDIUM | `registerModEntity` with legacy names | No DataFixer (Ev. 4) | Policy decision | static |

### Per-record details

| ID | Reproduction | Severity | Depends on | Stabilize before Modern? | Candidate regression tests |
|---|---|---|---|---|---|
| ENT-01 | `/summon` each registered name; registry dump | LOW | ENT-12 | no (IDs frozen) | Boot-time loop: every `ShipCalc` entity name resolves |
| ENT-02 | Set `Death_Time=0`, kill a tamed ship: no egg. Default 400: egg after about 20 s. | HIGH at 0, else LOW | ENT-07 | yes (data loss) | `shouldDropEgg(deathTime, max, canDrop)` at 0, 1, 400 |
| ENT-03 | Survival: use an egg in a state where spawn fails; check item and XP | MEDIUM | ENT-05 | yes (birth path) | `rollShipType` distribution after isolating the NBT read |
| ENT-04 | Ocean biome, ring, low `Cooldown_Boss`: fleet and chat announcement. Repeat with `Check_Ring=false` and no ring. | MEDIUM | ENT-06 | no (spec first) | `getRandomMobToSpawnName` tiers with an injected RNG |
| ENT-05 | Build with known materials; sample the egg class distribution | MEDIUM | ENT-03 | yes | Distribution test |
| ENT-06 | `Despawn_Minion=100`, walk away; drop a tagged egg and wait | LOW | ENT-04 | no | `canDespawn` predicate |
| ENT-07 | Sink a ship; `/shipget get <uid>` within 64 blocks; compare positions | MEDIUM | ENT-02 | yes (UID and cache format) | `checkShipIsDupe` decision table |
| ENT-08 | Owner at 20 blocks: GUI values and timers stop updating | MEDIUM | NET-09 | yes (trivial; sync contract) | `withinSyncRadius` helper |
| ENT-09 | Ride a mounted ship (e.g. Airfield Princess); save, quit and reload | MEDIUM | ENT-07 | yes if player riding at logout must be safe | Manual |
| ENT-10 | Kill a carrier mid-sortie; reload mid-flight | LOW | none | no | Lifetime predicates |
| ENT-11 | Chunk-loader module, then leave; lead a following ship through a Nether portal | MEDIUM | ENT-07 | no (needs a decision) | Manual |
| ENT-12 | Load a world saved by a pre-1.12 build | HIGH if in scope | all persistence | yes (decide before Modern IDs) | Old-save fixtures |

## Assumptions
- A1: `WorldServer.spawnEntity` rejects a duplicate UUID, so the second spawn only logs (ENT-03).
- A2: Vanilla `despawnEntity` calls `canDespawn()` (ENT-06).
- A3: 1.12.2 `ResourceLocation` lowercases paths, so the registry name is `shincolle:entitydestroyeri` (ENT-12).
- A4: Most players use default config, so ENT-02's HIGH severity applies only to `Death_Time=0`.

## Unknowns
- U1: Exact runtime registry names (needs a registry dump).
- U2: Whether `hasTagCompound()` is false on a stack shrunk to 0 (ENT-03 roll).
- U3: Whether the double spawn duplicates the ship.
- U4: Shipyard tile output.
- U5: Interaction of Metamorph morph ships (`CanDrop` forced false at `EventHandler.java:1085,1095`) with the death and cache paths.

## Risks
- ENT-02 and ENT-12 can lose or orphan player ships, which is persistent player data.
- ENT-08 makes owner GUIs show stale values beyond 8 blocks and may be mistaken for other sync bugs.

## Minecraft-independent logic candidates
`ShipCalc` (`rollShipType` weighting, `getRandomMobToSpawnName`, class-ID → name map); the death/drop predicates and the hostile `getDropEgg` chances (`BasicEntityShipHostile.java:202-213`); `canDespawn`; summon lifetime; the `checkShipIsDupe` decision; spawn-gate arithmetic.

## Recommended next actions
1. Stabilization candidates: ENT-08 (trivial, verified regression), ENT-02 (data loss), ENT-03.
2. Maintainer decisions: ENT-04 (ring gate), ENT-07 (revive position), ENT-11 (portals), ENT-12 (old-world migration).

## Phase 3 updates
- 2026-10-13 ENT-13 added. **Friendly ships are deleted when animal spawning is off.** With `server.properties` `spawn-animals=false` or `spawn-npcs=false`, vanilla `WorldServer.updateEntityWithOptionalForce` (`WorldServer.java:847`, Cleanroom 0.6.10 mapped source; the repo builds with 0.6.13, so line numbers may differ) calls `setDead` on `EntityAnimal` entities on their first tick. `BasicEntityShip` extends `EntityTameable`, which extends vanilla `EntityAnimal` (vanilla hierarchy, MEDIUM-HIGH), so ships are deleted.
  - **Fix state: verified, uncommitted.** Mixin exemption on `WorldServer` (`@WrapWithCondition` on the sweep's `setDead` call), with `use_mixins=true`. Final regression-verifier check (HEAD `7b91e2d` plus working tree): unit tests 21/0, build exit 0, dedicated harness red 3/2 (no server log kept for the red run), green 5/0, normal 8/0. Verified in the dev environment only; the reobfuscated jar has not been run on a server.
  - **Production check (2026-10-13, patch application only):** Cleanroom 0.6.13-alpha server at `C:\cleanroom server`, copy in `run/server-prod-check/` (untracked), reobfuscated jar, `spawn-animals=false`, `spawn-npcs=false`. CleanMix applied `MixinWorldServer`; `javap` shows both `setDead` calls in `func_72866_a` guarded by `shincolle$allowSweep`; cow and pig were removed. **Ship survival was not tested** (`/summon shincolle:EntityDestroyerI` NPE, see ENT-15). Human accepted the patch-plus-dev-harness proof for #10; recorded as the human's decision.
  - **Mixin compatibility level changed (human-approved):** `shincolle.mixin.json` `JAVA_8` to `JAVA_25`, after a production warning ("Class version 69 required ... JAVA_8 supports class version 52"). Production re-run: warning 0 times, patch still applied, cow and pig still removed. Test and build exit 0. Dev spawn-off dedicated harness re-run with `JAVA_25` on a fresh world: `SUMMARY passed=5 failed=0`; `dedi-spawnoff-ship-survives` passed; 0 "Class version" warnings and no mixin config errors; only the two known dev warnings (config not in a jar; refmap not read in dev). Results: `docs/porting/reports/logs/2026-10-13-gametest-dedicated-issue10-compat-spawnoff-results.txt`.
  - Report: `docs/porting/reports/2026-10-13-issue-10-spawn-sweep.md` (section "Production server check"). Report: `docs/porting/reports/2026-10-13-issue-10-spawn-sweep.md`. Status is unchanged; see below.
  - **Confirmed (current 1.12.2, HEAD)**
    - Stack trace and a one-variable flip on the dedicated-server harness (`runClient -Pgametest` flow). Confidence HIGH. The harness needs both flags set to true (as recorded in the Phase 3 run).
    - `DedicatedServer.java:162-163` reads `spawn-animals` and `spawn-npcs` with a default of `true`. `IntegratedServer.java:170-171` forces both to `true`, so single-player and LAN are not affected.
    - Scope: only `BasicEntityShip` and its subclasses, about 47 friendly ships (count as supplied; counting method not recorded). Hostile, mount, airplane and summon classes are not in this chain.
    - Supplied evidence (not re-checked here): the Forge 1.12.x WorldServer patch does not touch `updateEntityWithOptionalForce`.
    - Persistence: `BasicEntityShip.setDead` (`BasicEntityShip.java:4342-4350`) writes a `CacheDataShip` with `isDead=true` and the full NBT through `ServerProxy.setShipWorldData`. Whether that copy is saved to disk or can be recovered is **Unknown** (see Unknowns).
    - Status: CONFIRMED_BUG (current behavior). Confidence HIGH.
  - **Historical (upstream and vanilla; the 8bca563 "not checked" note is superseded)**
    - Superclasses, upstream PinkaLulan/ShinColle `mc-1.7.10` (head 9312511) and `mc-1.10.2` (head 4546ff5): `BasicEntityShip extends EntityTameable` in both. `BasicEntityShipHostile extends EntityMob`, `BasicEntityMount extends EntityCreature`, and `BasicEntitySummon extends EntityLiving` (1.10.2 and later). Confidence HIGH.
    - The repo's own history has the same `BasicEntityShip` superclass from 7c48a1e (2016-11-24) through 0aad501 (the 1.12.2 init commit) to HEAD (`BasicEntityShip.java:73`). It never changed. Confidence HIGH.
    - Vanilla: `WorldServer.updateEntityWithOptionalForce` already deleted `EntityAnimal`, `EntityWaterMob` and `INpc` entities when the matching spawn setting was off, in 1.7.2 and 1.9. These are third-party MCP dumps (secondary sources): https://github.com/xgraza/nebula-1.7.2/blob/1c010901ce9733fc73a70d34c7a1a67a356d2851/src/main/java/net/minecraft/world/WorldServer.java (lines 632-645) and https://github.com/CircuitLord/Minecraft-1.9-MCP/blob/bc89baf1fd0b5d422478619e7aba01c0b23bd405/src/minecraft/net/minecraft/world/WorldServer.java (lines 833-845). Confidence MEDIUM-HIGH.
    - Upstream never had a workaround: no mixin, no coremod, and no spawn-animals handling.
    - Upstream issues #26, #30 and #40 report ships vanishing on servers, with no diagnosis. Their link to this bug is **Unknown**.
    - Inference (no runtime test of old versions): servers running 1.7.10 or 1.10.2 with `spawn-animals=false` would also have lost friendly ships. Confidence not assigned; this is an inference only.
  - **Desired (human decision, 2026-10-13)**
    - Protect ships from this sweep. This departs from upstream, which did not protect them.
    - Approach: a Mixin exemption on `WorldServer`. The human approved enabling Mixin in the build.
    - Rejected alternatives: (1) a `setDead` guard in ship code, which is fragile because ship code cannot tell this call from a real death; (2) a superclass change, which is a large refactor with a risk to saved data; (3) documentation only.
  - **Assumptions**
    - A-ENT13-1: The vanilla sweep above is the deletion path for ships in this report. Not checked against other despawn or removal paths.
    - A-ENT13-2: Mounts (`EntityCreature`), summons (`EntityLiving`) and hostile ships (`EntityMob`) are not `EntityAnimal` and so are not hit by this check. Derived from the superclass chain; not tested.
  - **Unknowns**
    - U-ENT13-1: Whether the `CacheDataShip` copy written by `setDead` is saved to disk or can be recovered.
    - U-ENT13-2: Whether the NPC check (`spawn-npcs`) affects any ship class; ships are known only to be `EntityAnimal`.
    - U-ENT13-3: Whether upstream issues #26, #30 and #40 are this bug.
    - U-ENT13-4: Whether the 1.7.10 and 1.10.2 servers actually lost ships (no runtime test).
    - U-ENT13-5: Partly resolved. The Mixin exemption is verified in the dev environment (dedicated harness: spawn-off red 3/2, green 5/0, normal 8/0 on the `@WrapWithCondition` version). Still open: the reobfuscated jar has not been run on a real server; no explicit "mixin applied" log line exists; coexistence with another mod wrapping the same call is untested.
  - **Next actions**
    - Run the reobfuscated jar once on a real dedicated server with both spawn flags false and record the log path. Not run; needs human go-ahead. The dev harness red and green runs are recorded in the report.
    - Route the Unknowns above to the parent for review.
- 2026-10-13 ENT-14 added (low impact). **Double spawn from the egg.** `EntityHelper.createShipEntity` (`EntityHelper.java:1676`) already spawns the entity, and `ShipSpawnEgg.java:473` spawns it again. The second spawn is rejected with the "Keeping entity ... already exists" warning. The join event fires before `initEntityAttribute`, so HP is 4.0 at join; this explains the task-0 "Health 4.0" oddity.
  - Historical (8bca563): not checked.
  - Status: not assigned. Observed on the harness; impact described as low. Confidence not stated in the supplied evidence; not separately checked.
- 2026-10-13 ENT-15 added. **`/summon` of a ship throws an NPE when ShipExtProps NBT is missing.** `/summon shincolle:EntityDestroyerI` on the production-style server (Cleanroom 0.6.13-alpha, reobfuscated jar, `spawn-animals=false`) throws an NPE at `CapaShipSavedValues.java:129` (`loadNBTData`), called from `BasicEntityShip.readFromNBT` (`BasicEntityShip.java:585`) because the ShipExtProps NBT is missing.
  - Evidence: production server log and results, `docs/porting/reports/logs/2026-10-13-issue10-prod-server.log` and `docs/porting/reports/logs/2026-10-13-issue10-prod-results.txt`. Reproduced on the production server. Confidence HIGH.
  - Tracking: GitHub #11, https://github.com/yor42/ShinColle/issues/11
  - Historical: Unknown whether `/summon` of a ship worked in upstream 1.7.10 or 1.10.2.
  - Current 1.12.2: NPE on the `/summon` path as above (HEAD of the production check not recorded separately; the NPE is on the tested reobfuscated jar).
  - Desired: Unknown (not decided here; fix not proposed or approved).
  - Status: CONFIRMED_BUG. Confidence HIGH.
  - Unknowns:
    - U-ENT15-1: Whether `/summon` worked upstream (no historical run recorded).
    - U-ENT15-2: Whether other NBT-driven creation paths (for example the Ship Spawn Egg birth path, `EntityHelper.createShipEntity`, or `/shipget` revive) reach the same `loadNBTData` code with missing ShipExtProps NBT. Not checked.
    - U-ENT15-3: Ship survival of the #10 sweep fix on the production server is blocked by this NPE; not tested.
