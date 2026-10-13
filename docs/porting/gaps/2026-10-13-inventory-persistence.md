# Gap Analysis: Inventory and Persistent State (INV-)

- Date: 2026-10-13
- Phase: 2
- Author: Claude Code (legacy-code-analyst subagent; INV-03 and the baseline lineage spot-verified by the main session)
- Related issue/ADR: backlog `2026-10-13-gap-backlog.md`

## Scope
Ship inventory (pages, drums), item pickup, GUI containers, ship NBT (`CapaShipSavedValues`), ship-as-item (Ship Spawn Egg NBT), the player capability (`CapaTeitoku`), world saved data (`ShinWorldData`, `ServerProxy`), tile entity NBT, and config persistence. Static analysis only. NBT keys were compared by extracting every literal `get/set/has<Type>("…")`, `removeTag("…")` and `NAME = "…"` from both revisions and diffing them; computed keys were covered by reading the code.

Note on the baseline: the subagent wrote that `8bca563` "sits after the 1.12.2 init commit". **That is wrong.** `8bca563` is not a descendant of `0aad501`, and its build targets `1.10.2-12.18.3.2221` (main-session check). See the backlog's lineage section.

## Evidence
1. NBT key literal diff `8bca563..HEAD`:
   - New keys: `fe_capacity`, `fe_energy`, `fe_maxExtract`, `fe_maxReceive` (`CapaForgeEnergyStorage`); `energy_EUCapacity`, `energy_EUEnergy` (`TileEntityCrane.java:376-377,411-412`); config `MC_HostileMob_IFF` (`ConfigHandler.java:224`).
   - Removed: `chgHB`/`orgHB`, which were already in commented-out code in 8bca563.
   - **No ship, player, egg, world-data or other tile key was renamed or removed.**
2. Main-session verification: 8bca563 `CapaShipSavedValues.java` has 5 `nbt_load != null` guards; HEAD has 0. Removed in d7349f8 (yor42, 2025-09-14).
3. Mekanism adds no NBT and no capability (`MekanismHelper.java:12`).

## Confirmed findings

| ID | Feature | Status | Conf. | Historical (8bca563) | Current (HEAD) | Desired (proposal) | Evidence |
|---|---|---|---|---|---|---|---|
| INV-01 | Crane energy loads as zero from pre-FE saves | CONFIRMED_BUG | HIGH (path exists) | No energy keys | `battery.readFromNBT` (`TileEntityCrane.java:374`) and `getDouble("energy_EUCapacity")` (`:376-377`) read absent keys as 0, overwriting constructor defaults (`:141-145`). The zeros are then persisted (`:410-416`), and later config changes never apply to that tile. `CapaForgeEnergyStorage` ignores its `transferRate` argument (`:11-13`). | Keep constructor defaults when keys are absent | static |
| INV-02 | `Tile_Crane` config resized from 2 to 5 values, which silently resets user values | CONFIRMED_BUG | HIGH | 2 values; same reset logic | `getIntArrayFromConfig` resets to defaults on a length mismatch (`ConfigHandler.java:65,467-477`) | Migrate the old 2 values into the first slots | static |
| INV-03 | Ship NBT load lost its null guards | CONFIRMED_BUG (fork regression d7349f8) | HIGH (removal); LOW-MEDIUM (reachability) | 5 sections guarded | `Minor`, `Display`, `Point`, `ShipFlags` and `Timer` are dereferenced directly (`CapaShipSavedValues.java:125-215`), so a missing section causes an NPE in `readFromNBT`. Likely triggers: hand-made `/summon` NBT and external chunk tools. | Restore the guards | Ev. 2 (verified) |
| INV-04 | Inventory page selection trusted from the client | CONFIRMED_BUG (inherited) | HIGH | Same | Pages unlock by `DrumState` (equipped drums), not by level. The page is clamped only to 0..2 on the server (`CapaShipInventory.java:32-39`); `SlotShipInventory` has no availability check (`:46-77`); `canInteractWith` always returns true (`ContainerShipInventory.java:63`). Overlaps NET-01. | Server validates the page against the unlocked count; owner or teammate check | static |
| INV-05 | More than 2 drums breaks paged size and the Crane | UNKNOWN_INTENT (reachability) | MEDIUM | Method existed; Crane loops are new | `getSizeInventoryPaged = 24 + DrumState*18` uncapped (`CapaShipInventory.java:68-73`). `getStackInSlotWithPageCheck` throws past 60 (`:90-94`). Crane loops use it (`TileEntityCrane.java:156,187,216,250,1376,1450`). | Clamp to `SlotPages-1` | static |
| INV-06 | Item pickup (KCFindstr duplication fix) | No status (needs runtime) | MEDIUM | Re-cast without an `isDead` recheck | The fix is present (`EntityAIShipPickItem.java:32,110`; 5536b30, merged as 10d2e1e, "not tested on a server"). Pickup requires an empty slot even for mergeable stacks. The sound is broadcast (`world.playSound(null, …)`). No code was found for "pickup sound loops on others' ships". | Runtime verification | static |
| INV-07 | Ship Spawn Egg with a tag but no `Attrs` throws | CONFIRMED_BUG | MEDIUM-HIGH | Same shape (not strictly diffed) | `nbt.getIntArray("Attrs")[0]` on an empty array (`ShipSpawnEgg.java:441-443`), e.g. an anvil-renamed or `/give` egg in survival | `length > 0` guard | static |
| INV-08 | Ship Spawn Egg keeps a subset of state | UNKNOWN_INTENT (probably the death-punishment design) | MEDIUM | Same keys | `saveShipDataToNBT` (`EntityHelper.java:1857-1916`) drops Exp, Kills, Morale, Food, ammo, grudge, guard, Task. The `punish=false` branch never sets the level (unreachable). | Maintainer confirms | static |
| INV-09 | World data caches full ship NBT, including inventory | UNKNOWN_INTENT | MEDIUM | Same | `shipData[].sNBT` = full `writeToNBT` (`ServerProxy.java:644-648`). `/shipget` can recreate from a stale cache, giving rollback or a duplicate window until `checkShipIsDupe` (both entities > 200 ticks). Static `ShinWorldData.nbtData` is never cleared (`:55-62`). | Policy decision | static |
| INV-10 | `CapaTeitoku` player capability | UNKNOWN_INTENT | MEDIUM | Same layout | `TODO: remove this capa, move to ServerProxy world data` (`CapaTeitoku.java:32`). Load errors are swallowed (`:326-392`). | Decide: capability or world data | static |
| INV-11 | Dead code and stubs near persistence | UNKNOWN_INTENT | HIGH (dead) | n/a | `MC_HostileMob_IFF` only logs; its list block is `if (false)` and contains the typo `"EntitySketelon"` (`ServerProxy.java:196-208`, 0ec3a8c/b2286f6). `CapaInventory.getStacksInSlots` uses `items[i]` instead of `items[i - slotStart]` (latent; callers pass 0). Hopper capability is disabled. Crane item-handler capability returns false. | Record; no action | static |

### Per-record details

| ID | Reproduction | Severity | Depends on | Stabilize before Modern? | Candidate regression tests |
|---|---|---|---|---|---|
| INV-01 | Load a Crane saved before 914f4b9/364b282; read FE/EU capacity in the GUI | MEDIUM | config | yes (persistence contract) | `CapaForgeEnergyStorage` NBT round trip; legacy-NBT load with a seam |
| INV-02 | Launch with an old 2-value `Tile_Crane` config | LOW | none | no | Config-array helper test |
| INV-03 | `/summon` a ship with NBT lacking `ShipExtProps.Display` | MEDIUM (crash on entity load) | none | yes (cheap; blocks safe migration) | Missing-section load test via a ship-state DTO |
| INV-04 | Modified client sends a page change on a ship without drums | MEDIUM | NET-01 | no (policy) | `isSlotAvailable`, `clampPage` |
| INV-05 | Equip 3 drums (if possible), pair the ship with a Crane | HIGH if reachable | Crane | yes if reachable | `pagedSize(drums)` |
| INV-06 | Two players near a ship picking up items; dedicated server | LOW-MEDIUM | AI | verify before port | Insert and merge math |
| INV-07 | Rename a ship egg in an anvil; right-click in survival | MEDIUM | ENT-03 | no | `costLevel(attrs)` |
| INV-08 | Sink a ship; compare egg contents | LOW | ENT-02 | no | Egg encode/decode index mapping |
| INV-09 | Kill a ship's chunk, `/shipget`, reload the old chunk | MEDIUM | ENT-07 | decide before porting persistence | World-data round trip |
| INV-10 | Die, respawn, return from the End; check ring, team, morph supplies | LOW-MEDIUM | none | decide before port | Capability round trip |
| INV-11 | n/a | LOW | none | no | none |

### NBT and save-format inventory (all unchanged since 8bca563 unless marked)

**Ship entity** (`BasicEntityShip.java:574-617`; `CapaShipSavedValues`):

| Section | Contents |
|---|---|
| `ShipExtProps.Minor` | int: Level, Kills, Exp, NumAmmoL/H, NumGrudge, NumAirL/H, FMin, FMax, FHP, GuardX/Y/Z/Dim/ID/Type, PlayerUID, ShipUID, FType, FPos, Morale, Food, Crane, WpStay, AutoCR, Task, Side. string: `tagName` |
| `Display` | int: State, Emotion, Emotion2, Phase |
| `Point` | byte: HP, ATK, DEF, SPD, MOV, HIT |
| `ShipFlags` | bool: CanFloat, IsMarried, NoFuel, Melee, AmmoL/H, AirL/H, WedEffect, CanDrop, CanFollow, OnSight, PVPFirst, AA, ASM, PassiveAI, TimeKeeper, PickItem, HeldItem, AutoPump, IsMorph |
| other | `Timer.Crane` (int); `Owner` (string, only if non-empty); `uname` (string list); `CpInv` (ItemStackHandler: `Items`, `Size`) |
| not saved | `inventoryPage`; `DrumState` (recomputed) |

**Ship Spawn Egg:** `owner`, `ownername` (string), `PlayerID`, `ShipID` (int), `CpInv`, `Attrs` (int[7]), `Attrs2` (int[7]), `Flags` (byte[16]), `customname`.

**Player capability** `shincolle:TeitokuExtProps`: `hasRing`, `RingOn`, `RingFly`, `FormatID`, `MarriageNum`, `BossCD`, `PlayerUID`, `TeamCD`, `PlayerName`, `TeamList0..8`, `SelectState0..8`, `ColleShip`, `ColleEquip`, `CustomTargetClass`, `uname0..8`, `CpInv`, `NumGrudge`, `NumAmmoL`, `NumAmmoH`.

**World data** `data/shincolle.dat`: `nextPlayerID`, `nextShipID`, `UnatkTargetClass`, `playerData[]{pUID, CustomTargetClass}`, `teamData[]{tUID, tName, tLName, tBan, tAlly}`, `shipData[]{sUID, sEID, sWID, sCID, sDead, sPOS, sNBT}`.

**Tile entities:**

| Tile | Keys |
|---|---|
| `BasicTileInventory` | `pid`, `CpInv` |
| `BasicTileMulti` | `hasMaster`, `isMaster`, `masterX/Y/Z`, `structType` |
| `TileEntityCrane` | `active`, `paired`, `load`, `unload`, `meta`, `dict`, `nbt`, `mode`, `imode`, `rmode`, `lmode`, `emode`, `chestPos`, `lastPos`, `nextPos`, tank. **NEW:** `fe_*` (4 ints), `energy_EUCapacity`, `energy_EUEnergy` (double) |
| `TileEntityDesk` | `bookChap`, `bookPage`, `guiFunc`, `radarZoom` |
| `TileEntityLightBlock` | `life`, `type` |
| `TileEntitySmallShipyard` | `buildRecord`, `buildType`, `consumedPower`, `goalPower`, `remainedPower` |
| `TileEntityVolCore` | `active`, `power` |
| `TileEntityWaypoint` | `chestPos`, `lastPos`, `nextPos`, `pid`, `wpstay` |
| `TileMultiGrudgeHeavy` | `matsBuild`, `matsStock`, `buildType`, `invMode`, `powerConsumed`, `powerGoal`, `powerRemained`, `selectMat` |

**Config:** file paths unchanged; `MC_HostileMob_IFF` added; `Tile_Crane` resized from 2 to 5 (INV-02).

Not covered: item NBT of tools and books, and the internals of the sound, loot and mining configs.

## Assumptions
- Absent NBT keys read as 0 (vanilla behavior), so INV-01 holds.
- More than 2 drums may be equippable (INV-05). Per-ship equip-type limits were not traced.
- `ItemStackHandler` serializes under the same `Items`/`Size` keys as the 1.10.2 save format. Not verified.

## Unknowns
- Whether real 1.10.2 saves load: `ItemStackHandler` layout, item ID format. Needs an old-save fixture.
- Shipyard, desk and crane container slot rules.
- Integrated-server staleness of static `ShinWorldData.nbtData` across worlds.

## Risks
- **Save compatibility:** no ship, player, egg or world key changed. The persistence risks are behavioral (INV-01, INV-03), not format changes. Any fix must keep the key layout; persistent-format changes need approval.
- INV-03 can crash chunk loading for entities with partial NBT.
- INV-09 world-file size grows with fleets and inventories.

## Minecraft-independent logic candidates
Slot and page arithmetic; stack merge math (behind a stack interface); NBT layout DTOs for `ShipExtProps`, the egg arrays and `ShinWorldData`; `ServerProxy` ID allocation and dedupe policy; `TeamData` operations; Crane energy accounting; config-array validation.

## Recommended next actions
1. Build an NBT round-trip seam (ship-state DTO, egg arrays, `CapaForgeEnergyStorage`, world data) with legacy fixtures. This is a prerequisite for safe persistence fixes.
2. Stabilization candidates: INV-03 (restore guards), INV-01 (defaults on absent keys). Both change load behavior, not format.
3. Maintainer policy: INV-04, INV-05, INV-08, INV-09, INV-10.
