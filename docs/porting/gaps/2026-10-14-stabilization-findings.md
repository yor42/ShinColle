# Stabilization Findings: Filed Issues #13-#51

- Date: 2026-10-14
- Phase: 3 (Legacy Stabilization)
- Author: Claude Code (sonnet-implementer, issue filing)
- Related issue/ADR: ADR-0002 (save compatibility), ADR-0003 (Ship Morph legacy only), ADR-0004 (server-authoritative packets); backlog `2026-10-13-gap-backlog.md`; decisions `../decisions/README.md` (P-7, P-10, P-13 to P-17)

## Scope
This document records the 33 open findings from the Phase 2 gap reports and the Phase 3 task 0 report that were filed as triaged GitHub issues on 2026-10-14 (#13 to #45; the user approved creating these issues). Each issue has the labels `bug` plus one state label. It also lists the findings that were fixed earlier and one item that was deliberately not filed.

The labels `needs-info`, `needs-triage` and `ready-for-human` did not exist in the repository and were created (with the same names as in `docs/agents/triage-labels.md`) so the state labels could be applied. `needs-triage` is created but unused.

## Evidence
Every row below was re-checked against the current working tree on 2026-10-14 by reading the cited code. **All evidence is static code reading (confidence MEDIUM). Nothing in this document is runtime-verified, and no finding here is CONFIRMED_WORKING.** Statuses follow `../feature-status.md`: a finding that is "deterministic from code" is CONFIRMED_BUG only after its Phase 3 reproduction (test or recorded manual check) is done; until then it is a statically established finding.

Line numbers drifted slightly from the Phase 2 reports in places (for example `PacketHelper.java:311,377`, `EntityHelper.java:1689,1704`); the issue bodies cite the current lines.

## Constraints common to all findings
- No NBT key layout, registry ID, resource path or lang file may change (ADR-0002). Fixes that would need such a change are marked "approval" below.
- Fixes that change wire protocol only (packets) are allowed on the legacy track if client and server match; they still need a decision where marked.
- Each fix needs a characterization test or a recorded manual check first (Phase 3 rule).

## Confirmed findings

| Finding | Gap ID | Issue | State label | Confidence | Notes |
|---|---|---|---|---|---|
| Ship death drops no egg when `Death_Time` is 0 or within the first 10 ticks | ENT-02 | #13 | ready-for-agent | MEDIUM (static; HIGH that the code path exists) | Data loss only with `Death_Time=0`; config range change would need approval |
| Crane FE/EU capacity loads 0 from saves without energy keys | INV-01 | #14 | ready-for-agent | MEDIUM | Load behavior only; keys unchanged |
| `Tile_Crane` config 2 to 5 resize resets user values | INV-02 | #15 | ready-for-agent | MEDIUM | Migration decided in P-10 |
| Unbounded client-controlled array/string lengths in packets | NET-03 | #16 | ready-for-agent | MEDIUM | Wire-only; call-site audit incomplete |
| Ship Spawn Egg spends egg and XP before spawn | ENT-03 | #17 | ready-for-agent | MEDIUM | Interacts with #18 and #36 (same method) |
| Ship Spawn Egg with tag but no `Attrs` throws | INV-07 | #18 | ready-for-agent | MEDIUM | Only non-creative, damage > 1 |
| `ExpNext` not refreshed after Training Book / custom `expMod` | ATTR-07 | #19 | ready-for-agent | MEDIUM | |
| Fishing luck level bonus uses integer division | ATTR-08 | #20 | ready-for-agent | MEDIUM | Spot-verified in Phase 2 |
| Triple hit never triggers | CMB-01 | #21 | ready-for-agent | MEDIUM | P-7; characterization test first; changes balance |
| Day/night damage tables swapped | CMB-02 | #22 | ready-for-agent | MEDIUM | P-7; re-verified: `modDay` reads `ModDmgNight`; changes balance |
| Dodge clamp ignores "-1 = no limit" | CMB-06 | #23 | ready-for-agent | MEDIUM | |
| Path following: `yOffset` uses x; stuck-recovery sign | AI-04 | #24 | ready-for-agent | MEDIUM | |
| Ghost-HP code compares strings with `==` | RND-07 | #25 | ready-for-agent | MEDIUM | Intent of the block unknown (MOD-02); agent asked to report if unclear |
| S2C handlers: unchecked casts / null world; `ringAbility` never restored | NET-10 | #26 | ready-for-agent | MEDIUM | Wire-compatible |
| Unit rename calls no-op sync; `/ship list` page<=0 gives a 30000-entry page | NET-12 | #27 | ready-for-agent | MEDIUM | `sendSyncRequest` body is client-only |
| Abyssal boss cooldown requires ring even when `Check_Ring=false` | ENT-04 | #28 | ready-for-agent | MEDIUM | P-15 decided it is a bug; re-verified `EntityHelper.java:2070` |
| Forged client actors and unauthorized ship/tile actions | NET-01, NET-06, NET-07, INV-04 | #29 | ready-for-human | MEDIUM | ADR-0004; authorization model needs design; wire change possible |
| Partial-NBT ship fields load as 0 (Morale, UIDs, ammo, bonuses) | INV-03 follow-up (#7), AI-10, INV-08 | #30 | ready-for-human | MEDIUM | Additive defaults only; which key gets which default is a decision (ADR-0002) |
| `/shipget` recreates a ship from a stale world-data cache | INV-09 | #31 | ready-for-human | MEDIUM | Policy decision; save-format risk, approval |
| Auto emote on cancelled chat, opt-out not persisted, substring matching | AI-08 | #32 | ready-for-human | MEDIUM | P-10 / P-17, Phase 5; new capability key needs ADR-0002 approval |
| Bowl/container items not returned on feed | ATTR-04 | #33 | ready-for-human | MEDIUM | P-10 says bug, low priority; return-item list to confirm |
| BuffHelper docs disagree with code (Nausea, Water Breathing) | CMB-09 | #34 | ready-for-human | MEDIUM | Code vs comment: balance decision |
| `toBytes` reads/mutates live state off-thread | NET-05 | #35 | needs-info | MEDIUM that the code path exists; LOW that a race occurs | Race is an assumption; needs reproduction on a busy server |
| Egg double `spawnEntity`; NBT read after spawn (HP 4.0 at join) | ENT-14, ENT-07 | #36 | needs-info | MEDIUM | ENT-07 revive position decided in P-15 (cached position) |
| Stationary guard fires regardless of `PassiveAI` / `GuardType == 0` | AI-02 follow-up (surfaced by #3) | #37 | needs-info | MEDIUM | Intended behavior unknown |
| Guard idle start check effectively every ~96 ticks | AI-02 / P-14 | #38 | needs-info | LOW | Poll interval of 3 ticks assumed from commit notes |
| Training Book on a morph ship ignores the marriage cap | ATTR-06 | #39 | needs-info | MEDIUM | ADR-0003 (fix on legacy track); cap rule via `getLVCap()` needs decision |
| Resistance potion does not apply to missiles | CMB-03 | #40 | needs-info | MEDIUM | Cause established in the gap report (damage source is the ship); fix approach open |
| Crafting task reads 9 recipe entries regardless of list length | AI-09 | #41 | needs-info | LOW-MEDIUM | Suspected only; needs reproduction |
| More than 2 drums breaks paged inventory and Crane | INV-05 | #42 | needs-info | MEDIUM | Reachability of `DrumState > 2` unknown |
| Unguarded `MetamorphHelper` calls may throw `NoClassDefFoundError` | MOD-01, NET-11 | #43 | needs-info | LOW | Needs a launch without Metamorph (dev-runtime change needs approval) |
| Packet codec read/write asymmetries | NET-08 | #44 | needs-info | MEDIUM | Wire-only; codec redesign scope undecided; line numbers approximate |
| Hand-made ships vanished after restart (3 of 5) | Phase 3 task 0 Unknown (no gap ID) | #45 | needs-info | LOW | Hypothesis: ENT-02 first-10-ticks / fall damage before `CanDrop`; unproven |

## Intentional behavior recorded, not filed
- `TeamHelper.java:251` uses `player.getDistanceSq(tile) <= 64`. This is intentional: the code comment says "8 blocks" and the same code exists in upstream `8bca563` (`TeamHelper.java:275-278`). It was re-read on 2026-10-14 and left as is. It is not a bug and no issue was filed.

## Already fixed (not re-filed)
Issues #1 to #11 are closed. Fix commits:

| Issue | Subject | Fix commit |
|---|---|---|
| #1 | Item duplication in `getAndRemoveItem` | a523d5e |
| #2 | Server-side OP check for ship owner/attribute packets | 2403673 |
| #3, #4, #5, #6, #8 | Guard AI, Metamorph supplies, sync radius, depth, sounds | 4f74ad0 |
| #7, #11 | Ship NBT load tolerates missing `ShipExtProps` and sections | b88180a |
| #9 | Entity id 0 accepted in entity lookups | 7b91e2d |
| #10 | Ships kept alive when spawn-animals/spawn-npcs is off | 246e261 (follow-up 3a91033: mixin `compatibilityLevel` JAVA_25) |

## Assumptions
- Absent NBT keys read as 0 (vanilla behavior) (#14, #30).
- The guard AI poll interval is about 3 ticks (#38).
- Line numbers in issues were re-read on 2026-10-14; they will drift as the fixes land.

## Unknowns
- None of the findings has a runtime reproduction. Whether #45 is explained by #13 is open.
- Reachability of `DrumState > 2` (#42), of the Metamorph class-loading failure (#43), and of the thread race (#35).
- Legacy 2-value `Tile_Crane` layout (#15) and legacy Crane saves (#14).

## Risks
- Fixes for #21 and #22 change combat balance (P-7 approved them, with characterization tests first).
- #29, #30, #31 and #32 can touch persistence or the protocol; each needs the approvals named above before implementation.
- #17, #18 and #36 edit the same egg-use method; land in a coordinated order.

## Recommended next actions
1. Maintainer reviews the ready-for-human issues (#29 to #34) and answers the open questions in the needs-info issues (#35 to #45).
2. Phase 3 workers take ready-for-agent issues (#13 to #28), starting with #13 (data loss), #17/#18 (egg path) and #21/#22 (P-7).
3. Each fix lands with its characterization test or recorded manual check; no commits or pushes without the user's request.

## 1.10 baseline comparison
- Date: 2026-10-14
- Baseline: upstream ShinColle 1.10.2 source at commit `8bca563` (historical source, not observed gameplay)
- Confidence: MEDIUM (static reading and content diff only; nothing was run)

### Coverage and method
Content diff of the upstream tree at `8bca563` (938 files) against the current tree (1221 files), followed by reading the cited code on both sides. Present in the current tree, by count or by key comparison:
- Blocks: 17. Item classes: 40 (35 registered). Tile entities: 8. Entities: about 80. Recipes: 103. Commands: 9. GUIs, loot tables and the config options (91 `ConfigHandler` fields) are all present.
- `sounds.json`: 0 old keys missing, 0 old ogg files missing.
- Lang: 0 old keys missing in every language; `ko_kr` is new.
- Only 8 old paths are absent: `RenderDesk`, `RenderSmallShipyard`, `intermod/MetamorphHelper` (moved), `reference/Reference` (replaced by `Tags`), `BlockDesk.png`, `BlockSmallShipyardOff.png`, `BlockSmallShipyardOn.png`, and the root `mcmod.info` (moved).
- About 300 asset files were renamed to lowercase; this is required on 1.12 and is not a finding.

### New issues from the comparison
| Finding | Issue | State label | Confidence |
|---|---|---|---|
| Ship Tank right-click NPE in open air; air-pour request never sent | #46 | ready-for-agent (null guard only; restoring the air-pour feature is a separate decision) | MEDIUM |
| Large Shipyard may drop its block item twice; material WATER to SAND | #47 | needs-info | MEDIUM (double drop is an assumption) |
| Polymetal ore dictionary names always registered (`polyAsMn` else branch removed) | #48 | needs-info | MEDIUM |
| Combat Ration 4 milk loop registers every recipe as `ration6` | #49 | needs-info (fix renames registry IDs, needs approval and a naming decision) | MEDIUM |
| Shipyard fuel magnification truncated to int for item fuel | #50 | ready-for-agent | MEDIUM |
| Floating AI: hostless mount no longer floats up | #51 | needs-info | LOW |

### Recorded, not filed (probable deliberate changes)
- N-2: waypoint block client feedback removed. 1.10 `TileEntityWaypoint.java:109-210` versus now `:95-114` (commit cb6fbe4); `RenderWaypoint` added; the Pointer item is no longer a trigger; the F/T/C text and the chain/chest sprays are lost.
- N-3: legacy `IFluidContainerItem` / `FluidContainerRegistry` fuel paths are gone (`TileEntityHelper.java:125-128` is capability only; those APIs do not exist in 1.12).
- N-4: ore and gravel worldgen runs only in dimension 0 (the 1.10 default branch covered other dimensions).
- N-10: the Desk and Small Shipyard TESRs were replaced by JSON models, and the Waypoint became `ENTITYBLOCK_ANIMATED`.
- N-11: `BlockWaypoint` creative break no longer returns the item (`BlockWaypoint.java:206-218`).
- D6: creative tab icon changed to `BlockVolCore`.
- Fork additions: Tinkers, Mekanism, TOP, IC2 and FE integrations, `MC_HostileMob_IFF`, `drumFE`, crane FE fields, `EquipCannon` meta 16, `ko_kr`, `PacketAuthorization`, `EntityIdGuard`, `SpawnSweepGuard`, `AutoEmote`, `MixinWorldServer`.

### Decision: 1.10-era world loading is NOT in scope (human decision, 2026-10-14)
Entity and tile entity registry names moved to `ResourceLocation` `shincolle:<name>`, and only one DataFixer / MissingMappings handler exists (`molten_abyssium`). Worlds saved by 1.10 are unsupported and untested. This does not change ADR-0002 for saves made by the current 1.12.2 build.

### Not covered by the 1.10 comparison
- Per-class `calcShipAttributesAdd*` bodies were read only through a statement diff.
- `GuiDesk` and the other client GUIs.
- `ModRecipes`, `ShipCalc`, `EquipCalc`, `LargeRecipes` and `RecipeEnchantShell` bodies (about 100 changed statements; these could hide recipe or cost changes).
- `ParticleHelper`, `EntityHelper` and `TaskHelper` bodies.
- Rendering and model equality; item models were consolidated from per-meta to per-item files, and meta coverage is unverified.
- Potion, enchantment, key binding and capability NBT contents.
- 14 new ogg files that are not referenced anywhere.
- Any runtime confirmation.

### Assumptions
- `Item.rayTrace` returns null in open air (#46).
- The old `getDrops` signature is no longer consulted by 1.12 harvesting (#47).
