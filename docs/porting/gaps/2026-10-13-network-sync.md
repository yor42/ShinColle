# Gap Analysis: Network Synchronization (NET-)

- Date: 2026-10-13
- Phase: 2
- Author: Claude Code (legacy-code-analyst subagent; NET-02 and NET-04 spot-verified by the main session)
- Related issue/ADR: backlog `2026-10-13-gap-backlog.md`

## Scope
All 6 packet classes in `network/` (about 3,970 lines), `proxy/CommonProxy.java`, `utility/PacketHelper.java`, relevant parts of `EntityHelper`, `FormationHelper`, `ServerProxy`, `CapaTeitoku`, `MetamorphHelper`, `TileEntityHelper`, and the login, dimension-change and clone hooks in `EventHandler`. Compared with `8bca563`. Static analysis only; nothing was built or run. Security findings describe the class of problem, not exploits.

Paths: `N/` = `src/main/java/com/lulan/shincolle/network/`, `U/` = `.../utility/`.

## Evidence
1. Four `SimpleNetworkWrapper` channels are registered at `proxy/CommonProxy.java:79-96`: `scEnt`, `scGui`, `scPar`, `scIn`.
2. Every handler schedules on the main thread through `addScheduledTask`: C2SGUI:687, C2SInput:421, S2CEntitySync:1206, S2CGUI:1055, S2CReact:189, S2CSpawnParticle:380. This matches 8bca563 and was introduced by 2ba1936 (2016).
3. Entity state sync uses hand-written packets. `DataParameter` is used only in `item/BasicEntityItem.java:42`.
4. Ship sync goes only to the owner, within a squared distance of 64 (`BasicEntityShip.java:1478-1495`).
5. **Main-session verification (2026-10-13):**
   - HEAD `intermod/metamorph/MetamorphHelper.java:716` ends with `return itemGet.isEmpty();`, while 8bca563 `intermod/MetamorphHelper.java:812-813` has `if (itemGet == null) return false; else return true;`.
   - `InventoryHelper.getAndRemoveItem` returns `ItemStack.EMPTY` when nothing is found (`U/InventoryHelper.java:506-510`).
   - The `C2SInputPackets` cases `CmdChOwner` and `CmdShipAttr` (around lines 118-159) contain no `checkOP` or `canUseCommand` call.

## Confirmed findings

| ID | Feature | Status | Conf. | Historical (8bca563) | Current (HEAD) | Desired (proposal) | Evidence |
|---|---|---|---|---|---|---|---|
| NET-01 | C2S actor identity and target ownership | CONFIRMED_BUG (inherited) | HIGH | Same structure; the real sender is used once (C2SGUIPackets:723) | The player is resolved from a client-supplied entity ID and dimension (`C2SGUIPackets:150,177,224,236,264,279,298,313,343,376,393,413`). `ShipBtn` → `PacketHelper.setEntityByGUI` has no owner check (`U/PacketHelper.java:405-553`). `SetSitting` toggles any ship by UID with no null check (`FormationHelper` :958-962). `currentItem` is set without a 0-8 bound (C2SGUI:240,313; C2SInput:116). | The actor comes only from the message context. Each target is authorized against it (owner, reach, loaded chunk). Indices are bounds-checked. | static; good pattern at C2SInput:78,89,188 |
| NET-02 | OP-only command packets | CONFIRMED_BUG (inherited) | HIGH | Identical; comments claim an OP check that is not there | `CmdChOwner` (:118-135) and `CmdShipAttr` (:136-159) have no server-side permission check. `EntityHelper.checkOP` returns true for any creative-mode player (`U/EntityHelper.java:202`). | Server-side `canSendCommands`; no creative shortcut for server-wide edits | static; verified (Evidence 5) |
| NET-03 | Unbounded client-controlled lengths | CONFIRMED_BUG | MEDIUM | Not established | `readIntArray`/`readByteArray` allocate `new int[length]` before reading (`U/PacketHelper.java:310,376`); lengths come from the wire (C2SGUI:493,517-519; C2SInput:353; S2CSpawnParticle:138-139). Strings are unbounded. A negative length throws `NegativeArraySizeException`. | Cap lengths by `readableBytes()` and a per-type maximum; cap string length | static |
| NET-04 | Morph supply buttons consume and grant inverted | CONFIRMED_BUG (port regression, null→`EMPTY`) | HIGH | `return itemGet != null` semantics (correct) | `return itemGet.isEmpty()`: resources are granted when the item is absent, and the item is lost without credit when present. Reached through `MorphBtn2` (C2SGUI:126-133) from `GuiMorphInventory.java:1112-1118`. Matches Bugs.txt line 10. | `return !itemGet.isEmpty()`, plus an owner check | static; verified (Evidence 5) |
| NET-05 | `toBytes` reads and mutates live game state | CONFIRMED_BUG | MEDIUM | Same layout | `S2CReactPackets.CmdShipList.toBytes` calls `CommandHelper.processSendShipList`, which accesses the world and mutates entity caches (`U/CommandHelper.java:169-188`). `SyncPlayerProp_TeamData` iterates live team data (S2CGUI:639-660). `SyncShip_Attrs.toBytes` clears update flags (S2CEntitySync:866-873). | Snapshot data in the constructor on the server thread; `toBytes` only serializes | static; the thread race is an assumption |
| NET-06 | Waypoint and chest pairing trust a client-supplied UID | CONFIRMED_BUG | MEDIUM | Not established line by line | UID taken from `value3[0]` (C2SInput:195-253). Distance is checked only between the two blocks. Owner comparison against the claimed UID (`U/TileEntityHelper.java:233`). No reach or loaded-chunk check. | Server-derived UID; reach and loaded-chunk checks | static |
| NET-07 | Tile GUI packets act at arbitrary positions | CONFIRMED_BUG (inherited) | MEDIUM | Same | `TileBtn` and `Desk_FuncSync` (C2SGUI:136-163) → `setTileEntityByGUI` (`U/PacketHelper.java:604-686`) with no open-container, reach or owner check | Require an open container bound to the tile, or reach plus owner check | static |
| NET-08 | Read/write asymmetry in edge paths | CONFIRMED_BUG | HIGH | Not established | `SyncEntity_Host` writes only for fishing hooks but always reads (S2CEntitySync:718-720,1153-1161). Null `UnitName`/`Buffmap` write nothing but the reader expects a size (:1135-1151). `Riders` has a latent NPE (:1021). Empty `Ints_Path` breaks decoding. `CmdShipList` writes nothing unless `valueInt[0]==0` but always reads 3 ints (S2CReact:150-156). A short `Attrs` array throws (:631-650). | Every path writes what `fromBytes` reads | static |
| NET-09 | Sync model: owner-push plus non-owner polling | UNKNOWN_INTENT | MEDIUM | No tracking-start hook either | Login sends only config (`EventHandler.java:818-823`). Capability sync happens per tick, on clone and on dimension change. Non-owners poll at ticks 40/120 and every 64/128 ticks (`BasicEntityShip.java:2034-2052`). Packets for not-yet-spawned entities are dropped (S2CEntitySync:526-529). | Maintainer decision (see Open questions) | static |
| NET-10 | Unchecked casts and null world in S2C handlers | CONFIRMED_BUG | MEDIUM | Not established | `getShipByEntity` casts any living entity (S2CEntitySync:90-92,576). `CustomData` casts (`U/PacketHelper.java:561-593`). `getClientWorld()` is not null-checked (S2CEntitySync:505; S2CGUI:136). `SyncSystem_Config` overwrites static `ringAbility` and never restores it (S2CEntitySync:514-523; `ConfigHandler.java:119`). | `instanceof` checks, null guards, restore client config on disconnect | static |
| NET-11 | Sidedness and Metamorph references in packet code | UNKNOWN_INTENT | LOW | Not established | S2C classes reference `ClientProxy` (no `@SideOnly`; calls `Minecraft.getMinecraft()`). `MetamorphHelper` is reached without an `activeMetamorph` guard (also MOD-01). | Dedicated-server and no-Metamorph verification | static |
| NET-12 | Minor issues | CONFIRMED_BUG (dead call) / UNKNOWN_INTENT (size) | LOW | n/a | `SetUnitName` calls `sendSyncRequest(1)` server-side, which does nothing (C2SGUI:484; `BasicEntityShip.java:1504-1506`). No S2C size guard: `/ship list` page <= 0 gives a 30,000-ship page (`U/CommandHelper.java:196-199`). `ID.Packets.TEST` and `S2C_PlayerSync` are unused. | Fix the dead call; bound list pages | static |

### Per-record details

| ID | Reproduction | Severity | Depends on | Stabilize before Modern? | Candidate regression tests |
|---|---|---|---|---|---|
| NET-01 | Dedicated server, two accounts; a modified client sends C2SGUI `ShipBtn`/`ClearTeam`/`Desk_Disband` naming account B or B's ship; observe B's state | HIGH (MP) | NET-02, NET-07 | yes: design the authorization model rather than copy it | Pure authorization predicate (actor, owner, distance, dimension) |
| NET-02 | Non-OP account sends `CmdShipAttr` for a ship; observe the level change | CRITICAL (public servers) | NET-01 | yes | Non-OP sender rejected (permission query injected) |
| NET-03 | Fuzz `Desk_FuncSync` and generic types with lengths -1 and `MAX_INT` | HIGH (public servers) | none | yes (codec) | Buffer fuzz per packet: clean failure, no OOM |
| NET-04 | Metamorph present: morph into a ship, open the morph inventory, click Add Grudge with an empty inventory (grudge rises); then with exactly one grudge item (item lost) | HIGH (balance) | MOD-01, NET-01 | yes: a one-line logic fix the port would otherwise copy | Pure consume-decision unit test |
| NET-05 | `/ship list` repeatedly on a busy dedicated server during logins; watch for `ConcurrentModificationException` or bad caches | HIGH (`CmdShipList`/`TeamData`), else MEDIUM | none | yes: defines payload records | Snapshot → `toBytes` → `fromBytes` equality |
| NET-06 | Send `Request_WpSet` with another player's UID near their waypoint | MEDIUM | NET-01 | yes | Validation-function unit test |
| NET-07 | Send `TileBtn` for a Crane 100 blocks away without opening its GUI | MEDIUM | NET-01, NET-03 | yes | "May edit tile" predicate |
| NET-08 | Encode `SyncEntity_Host` for a non-hook entity, or a ship with null `unitNames`, then decode | LOW | none | no (codec redesign) | Round-trip every packet type, null and empty included (`Unpooled.buffer()`) |
| NET-09 | Account B approaches A's ship; measure time to correct name, model and buffs; repeat after respawn and End→Overworld | MEDIUM | NET-05 | no (record the behavior first) | In-world dedicated-server check |
| NET-10 | A recycled entity ID or a stub server sends a mismatched packet | LOW | none | no | Codec validation tests |
| NET-11 | Dedicated server with no `debugMod/` jars; trigger a particle packet and `MorphBtn2` | HIGH if it fails | MOD-01 | verify first | Dedicated-server smoke test |
| NET-12 | Rename a ship; check a non-owner client | LOW | NET-09 | no | n/a |

### Packet inventory

| Class (channel, id) | Dir | Purpose | Main thread? | Validated? |
|---|---|---|---|---|
| `S2CEntitySync` (scEnt, 1) | S2C | ship and entity state (AllMisc, Emo, Flag, Minor, Riders, Scale, Formation, Buffmap, Timer, Guard, ID, UnitName, Attrs, …), projectile, config | yes | partly: null-gated, but unchecked casts (NET-10) |
| `S2CSpawnParticle` (scPar, 2) | S2C | attack and animation particles, path indicator | yes | little (NET-08) |
| `S2CGUIPackets` (scGui, 4) | S2C | tile GUI sync, player capability, team data, ship GUI | yes | partly |
| `S2CReactPackets` (scIn, 7) | S2C | command replies, ship list, flare | yes | no (NET-05) |
| `C2SGUIPackets` (scGui, 3) | C2S | ship, tile and morph buttons, team and desk management, OP tools, unit name | yes | no for identity, ownership or reach; OP only for `SetUnatkClass` |
| `C2SInputPackets` (scIn, 6) | C2S | mount keys, GUI open, commands, sync requests, riding, waypoint pairing, radar | yes | partly: owner checks on mount and riding only |

## Assumptions
- Netty may encode outbound messages off the server thread, which is what makes NET-05 a race.
- The Forge C2S payload cap (about 32 KiB) bounds the stream but not an allocation sized by a header field.

## Unknowns
1. The thread on which encode actually runs for these channels.
2. Whether a dedicated server loads the S2C classes and `MetamorphHelper` without client classes or Metamorph (NET-11).
3. Exact payload limits under Cleanroom.
4. Whether clone and login syncs reach a client that already has its new player entity.
5. Line-level equality of 8bca563 and HEAD waypoint pairing logic.
6. Whether the `/ship` command classes are the only intended path for NET-02.

## Risks
- NET-01, NET-02, NET-03 and NET-07 make a public multiplayer server untrustworthy. They are inherited from upstream, so they are not port regressions, but they block a safe Modern design.
- Fixing NET-08 or NET-05 changes the wire format. That is not persistent data, but client and server versions must match.

## Minecraft-independent logic candidates
- The codec helpers in `U/PacketHelper.java:47-386` (Netty `ByteBuf` only), with length bounds.
- Packet IDs and per-packet field layouts, as records.
- Authorization predicates over plain values (NET-01, -06, -07).
- Team, ally and ban rules in `ServerProxy` (:780-902).
- The supply consume decision (NET-04).

## Recommended next actions
1. Stabilization candidates: NET-04 (smallest, verified), NET-02, then NET-01/NET-07 with a shared authorization predicate. Each needs a characterization test first.
2. Maintainer decision on NET-09 (keep polling vs. tracking-start sync on the Modern track).
3. A dedicated-server, no-optional-mods smoke run (NET-11, MOD-01).

## Phase 3 updates
- 2026-10-13 NET-02 fix (issue #2; see `../reports/2026-10-13-issue-2-op-check.md`). Shared OP predicate `PacketAuthorization`; the two packets and two `/ship` commands use it. Creative non-OP players are now blocked (behavior change). Other `EntityHelper.checkOP` callers keep the creative shortcut (follow-up under NET-01 / NET-07).
- 2026-10-13 NET-13 added. **Entity id 0 is rejected.** `EntityHelper.getEntityPlayerByID` (`EntityHelper.java:294-303`) and `getEntityByID` (`:254-268`) require `entityID > 0`. The first entity on a fresh server, typically the first player to join, has id 0. So `C2SInputPackets.java:130`, `CommandHelper.processShipChangeOwner` (line 49) and `processSetShipAttrs` (line 119) fail silently for that player.
  - Evidence: confirmed on the dedicated server (harness workaround: eight throwaway entities before the player joins). Confidence HIGH.
  - Proposed fix: `>= 0`. Not applied or tested. Other `entityID > 0` sites need review.
  - Historical (8bca563): not checked.
  - Status: CONFIRMED_BUG (current behavior; reproduced on the dedicated server). Fix needs approval.
  - 2026-10-13 fix line (issue #9): `EntityHelper.getEntityByID` and `getEntityPlayerByID` now use `EntityIdGuard.isLookupable(id)` (`id >= 0`) at `EntityHelper.java:263` and `:303`. Report: `../reports/2026-10-13-issue-9-entity-id-zero.md`. Status: **verified, uncommitted** (regression-verifier, final snapshot at HEAD `2403673` plus the working tree: `test --rerun-tasks` BUILD SUCCESSFUL, 16 tests, 0 failures; `build` BUILD SUCCESSFUL; see the report's "Final verification"). Post-fix dedicated run: player at entity id 0, OP forged-packet control changed owner 9999 → 100 and level → 77 (`logs/2026-10-13-gametest-dedicated-issue9-results.txt`, 8/8 PASS). Pre-fix failure at id 0 is from code reading and this entry; no saved pre-fix run at id 0 (confidence MEDIUM in the report; this entry's HIGH is unchanged pending human decision).
  - 2026-10-13 #9b: the regression this created (crane and waypoint owner sent as 0 for "no owner", now resolved to the first player) is fixed by `EntityIdGuard.encodeOptional` at `S2CGUIPackets.java:735` and `:761` (writes -1 for null). Call-site audit done by the implementer (about 40 sites); R1 was the only 0-sentinel sender. Residual: mount/rider writers (see NET-14). Status: verified, uncommitted.

### NET-14 (2026-10-13): `S2CEntitySync` drops packets for entity id 0

- Status: fixed in #9, verified, uncommitted. Was UNKNOWN_INTENT, confidence LOW, when first recorded.
- Evidence: `src/main/java/com/lulan/shincolle/network/S2CEntitySync.java:91` reads `if (msg.entityID > 0) { entity = EntityHelper.getEntityByID(msg.entityID, 0, true); }`. The same `> 0` pattern as NET-13, on the client side. Static reading only.
- Why it matters (hypothesis): a Minecraft entity with id 0 (the first player on a fresh dedicated server was id 0 in the issue #9 run, `logs/2026-10-13-gametest-dedicated-issue9-results.txt:15`) would have its sync packets skip the entity lookup. Whether any sender places 0 in `entityID` to mean "none" (which would make `> 0` correct here) is not established. Whether a ship spawned first can hold id 0 is not tested.
- Historical (8bca563): not checked.
- Current (HEAD / working tree): `S2CEntitySync.java:91` unchanged by the #9 fix (out of scope for it).
- Desired: Unknown; needs a human decision on the intended meaning of 0 in this packet, after the sender audit.
- 2026-10-13 fix (#9b, R2): `S2CEntitySync.java:92` now uses `EntityIdGuard.isLookupable(msg.entityID)`; the writer at `:732-733` sends `-1` (`EntityIdGuard.NO_ENTITY`) for a null entity. Approved by the human as part of #9. Verified, uncommitted. Report: `../reports/2026-10-13-issue-9-entity-id-zero.md` (Follow-up #9b).
- Still open (from #9b): the mount and rider writers at `S2CEntitySync.java:403,411,427` send 0 for "no mount"; their receivers keep their own `> 0` checks. A vehicle with entity id 0 is ignored there. Needs a human decision before any change.
- Historical (8bca563) for this check: not checked.
