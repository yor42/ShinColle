# Gap Analysis: Mod Integrations (MOD-)

- Date: 2026-10-13
- Phase: 2
- Author: Claude Code (legacy-code-analyst subagent, compiled by main session)
- Related issue/ADR: ADR-0001; backlog `2026-10-13-gap-backlog.md`

## Scope
`src/main/java/com/lulan/shincolle/intermod`, every `Loader.isModLoaded` / `@Optional` / mod API use, `gradle/scripts/dependencies.gradle`, `debugMod/` (file names only), the `@Mod` dependencies string, and `mcmod.info`. Static analysis only: nothing was built or run. The historical baseline is upstream PinkaLulan 1.10.2, commit `8bca563`. The smoke log shows only that the mod loaded with Mekanism, TOP and Mantle present; it is not feature evidence.

## Evidence
1. The `@Mod` dependencies string is only `required-after:forge@[14.23.5.2768,)` (`ShinColle.java:36`). 8bca563 had `required-after:Forge@[12.18.3.2185,)` only.
2. `mcmod.info` template has no dependency fields (`src/main/resource-templates/mcmod.info:1-12`).
3. `CommonProxy.checkModLoaded` sets `activeBaubles`, `activeIC2` (also requires config `Mod_IC2`, default true) and `activeMetamorph` (`proxy/CommonProxy.java:58-77`; `ConfigHandler.java:67,276`).
4. Gradle scopes (`dependencies.gradle:26-34`): Mekanism CE 9.12.8, Mantle, TOP CE are `modImplementation`. mclib 2.4, metamorph 1.3, Baubles API, TiC, IC2 are `modCompileOnly`.
5. `debugMod/`: Baubles-1.12-1.5.2-api, Mekanism CE 9.12.8, mclib 2.4, metamorph 1.3.
6. Waila: only a commented-out IMC with `TODO` (`ShinColle.java:98-99`). No JEI, Forestry, or RF/CoFH code.
7. The Gendustry bee config exists twice: `intermod/gendustry/` (upstream, c36d4c4/52252be) and `src/main/resources/assets/gendustry/config/shincolle_bees.cfg` (6c92cc3, yor42).

## Confirmed findings

| ID | Feature | Status | Conf. | Historical (8bca563) | Current (HEAD) | Desired (proposal) | Evidence |
|---|---|---|---|---|---|---|---|
| MOD-01 | Metamorph optional-dependency guard | UNKNOWN_INTENT (possible CONFIRMED_BUG) | LOW | Same unguarded pattern (`S2CSpawnParticle.java:308-326`, `BasicEntityShip.java:2821,2957`) | Calls into `MetamorphHelper` (imports `mchorse.*`) without an `activeMetamorph` check: `S2CSpawnParticle.java:146,151,164,172`, `C2SGUIPackets.java:132`, `BasicEntityShip.java:2524,2637` (behind `isMorph`). Other call sites are guarded. | Every Metamorph reference sits behind the flag or behind an optional-implementation interface | static |
| MOD-02 | "Phantom ships" bug (Bugs.txt) | UNKNOWN_INTENT | LOW | Bugs.txt says the bug started in 1.10; not verified at 8bca563 | Suspects: `EventHandler.java:1003-1160` (`onMorphPre`, `onMorphAction`, `SpawnGhostEvent`) | Reproduce, then fix | Bugs.txt:4-6 |
| MOD-03 | Metamorph player ship skills and morph GUI | UNKNOWN_INTENT | LOW | Upstream 07ebf8a, 8bca563 | `ShipSkillHandler.java:449-451`; `C2SGUIPackets.java:127-133`; items check `enableMetamorphSkill` | Decide whether it carries forward; separate skill math from Metamorph glue | static |
| MOD-04 | IC2 EU in Crane | UNKNOWN_INTENT (candidate PLANNED_DEVIATION, fork-added) | LOW | "EU storage TODO NYI" (`TileEntityCrane.java:121,1094` at 8bca563) | Implemented with `@Optional.InterfaceList` and `@Optional.Method` (`TileEntityCrane.java:65-77,1822-1925`), behind `activeIC2` | Keep; verify the IC2-absent path | 364b282, 914f4b9 |
| MOD-05 | Forge Energy in Crane | UNKNOWN_INTENT (candidate PLANNED_DEVIATION) | LOW | Not found | `CapaForgeEnergyStorage.java`; `TileEntityCrane.java:40,180` | Keep; review NBT of the battery | 914f4b9 |
| MOD-06 | Baubles marriage ring | UNKNOWN_INTENT | MEDIUM | Same `@Optional` pattern (`MarriageRing.java:14-15,32-33`) | Guarded: `MarriageRing.java:31,176,182`; `InventoryHelper.java:936`; `EventHandler.java:614` | Keep | static |
| MOD-07 | Mekanism Enrichment Chamber recipes | UNKNOWN_INTENT (candidate PLANNED_DEVIATION) | LOW | None | `MekanismHelper.java:11-14`, guarded at `ShinColle.java:49-53` | Keep; verify in-game | 9e5f6a6 |
| MOD-08 | TiC Abyssium material and fluid | UNKNOWN_INTENT (candidate PLANNED_DEVIATION) | LOW | None | `TinkersInit.java:25-60`, guarded at `ShinColle.java:80-82,107-109`; `missingBlockMappings` remap for `shincolle.molten_abyssium` (`ShinColle.java:~120-130`) | Not established; registry ID must stay stable | cb6fbe4 |
| MOD-09 | The One Probe provider | UNKNOWN_INTENT (candidate PLANNED_DEVIATION) | LOW | None | `TOPInit.java:30-35,105-109`; imports the non-API class `apiimpl.styles.ProgressStyle` (line 8) | Keep; use the API package only | 91b47fb |
| MOD-10 | Gendustry bee config | UNKNOWN_INTENT | LOW | Config at `intermod/gendustry/` | A second copy in resources; which copy Gendustry reads is unknown | Not established | 6c92cc3 |
| MOD-11 | Waila/Hwyla, JEI | INCOMPLETE_PORT (Waila) | MEDIUM | Not established | Commented-out IMC with `TODO` that points to a nonexistent class | Not established; TOP covers the role | `ShinColle.java:98-99` |
| MOD-12 | Dependency declaration | UNKNOWN_INTENT | MEDIUM | Forge only | No `after:` soft deps. Mekanism, TOP and Mantle are `modImplementation`, so dev runClient never tests their absent-mod paths | Soft `after:` deps; optional mods compileOnly | Evidence 1, 4 |

### Per-record details (repro, severity, dependencies, stabilization, tests)

| ID | Reproduction | Severity | Depends on | Stabilize before Modern? | Candidate regression tests |
|---|---|---|---|---|---|
| MOD-01 | Remove metamorph and mclib, launch, join a world, and trigger a ship attack animation (S2CSpawnParticle `Entity_Animate`). Watch for `NoClassDefFoundError`. | HIGH if it throws, else LOW | none | yes: it sets the integration boundary | Launch-without-Metamorph smoke test; a classloader test that hides `mchorse.*` and loads `S2CSpawnParticle` |
| MOD-02 | With Metamorph: morph into a ship, demorph, and count leftover entities and name tags. Repeat without Metamorph. | MEDIUM | MOD-01 | yes | Manual morph/demorph entity count |
| MOD-03 | With Metamorph: use the morph GUI stat buttons and check item consumption (Bugs.txt:10) | MEDIUM | MOD-01, NET (GUI packets) | yes (carry-forward decision) | Unit tests on skill math after separation |
| MOD-04 | Without IC2: place a Crane, toggle energy mode. With IC2: test EU in and out. | MEDIUM | MOD-05 | no (verify the guard) | Launch without IC2; EU/FE math unit test |
| MOD-05 | Charge a ship at a Crane with an FE source; save and reload | LOW | none | yes (shared-logic candidate) | Pure charge/discharge rate test |
| MOD-06 | Without Baubles: wear the ring and check flight. With Baubles: put the ring in a bauble slot. | LOW | none | no | Launch without Baubles |
| MOD-07 | Without Mekanism: launch. With it: check the recipe in JEI or the machine. | LOW | none | no | Launch without Mekanism |
| MOD-08 | With TiC and Mantle: check melting and casting. Without TiC: launch. | MEDIUM (registry remap) | ModItems | yes (registry ID) | Missing-mappings remap test |
| MOD-09 | Look at a ship, Crane or Waypoint with TOP, then launch without TOP | LOW | none | no | Launch without TOP |
| MOD-10 | Forestry and Gendustry: check whether the bees exist | LOW | Forestry, Gendustry | no | none |
| MOD-11 | n/a | LOW | none | no | none |
| MOD-12 | Launch matrix (see next actions) | LOW-MEDIUM | MOD-01 | no | Launch matrix |

### Integration matrix

| Mod | In 8bca563? | Current? | Guarded? | Gradle scope | Origin |
|---|---|---|---|---|---|
| Metamorph (+MCLib) | yes | yes | partial (MOD-01) | modCompileOnly | 51707b0 / 312a56a |
| Baubles | yes | yes | yes | modCompileOnly (API) | upstream; jar via e611f2a, 8ae8723 |
| IC2 | flag only, EU NYI | implemented | yes, untested | modCompileOnly | 364b282 |
| Forge Energy | no | yes | n/a (Forge core) | none | 914f4b9 |
| Mekanism | no | recipes | yes | modImplementation | 9e5f6a6 |
| TiC / Mantle | no | yes | yes | TiC compileOnly, Mantle implementation | cb6fbe4 |
| The One Probe | no | yes | yes | modImplementation | 91b47fb |
| Forestry/Gendustry | config only | config plus a resource copy | n/a | none | c36d4c4, 52252be, 6c92cc3 |
| JEI, RF/CoFH | no | no | n/a | none | n/a |
| Waila/Hwyla | not established | commented TODO | n/a | none | n/a |

## Assumptions
- Missing-mod safety of `@Optional` relies on Forge ASM stripping, which is not tested at runtime.
- Whether the JVM loads `mchorse.*` when it verifies `S2CSpawnParticle` (MOD-01) is not tested.
- The Mekanism origin comes from `git log -S"MekanismHelper"`.

## Unknowns
- Runtime behavior of all integrations. Metamorph, Baubles, IC2 and TiC were not loaded in the smoke run.
- Which Gendustry config copy is read, and whether the two copies differ.
- Whether Abyssium fluid registrations from the TiC code and from `ModBlocks` can conflict.
- The cause of the phantom ship bug.

## Risks
- Absent-mod regressions are invisible in dev because Mekanism, TOP and Mantle are runtime dependencies there.
- MOD-01 may crash clients without Metamorph on the first attack animation.
- The TiC remap touches saved-world IDs, so any change needs approval.

## Recommended next actions
1. Run a launch matrix: base only; +Baubles; +Metamorph and mclib; +IC2; +TiC and Mantle. Enter a world each time, trigger a ship attack (MOD-01) and a Crane energy toggle (MOD-04), and record results.
2. Maintainer decisions: MOD-01 (is Metamorph optional?), MOD-12 (dev-only `modImplementation`?), MOD-03 and MOD-08 (Modern-track targets?).
3. Adding soft `after:` dependencies changes build metadata and needs approval.
