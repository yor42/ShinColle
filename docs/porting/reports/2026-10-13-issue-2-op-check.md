# Issue #2 (NET-02): Server-side OP check for ship owner and attribute packets

- Date: 2026-10-13
- Phase: 3
- Author: Claude Code (documentation worker; evidence supplied by the parent, which ran the implementer, unit-test and verifier steps)
- Related issue/ADR: issue #2 ("Server does not verify OP for ship owner/attribute command packets", OPEN; cites NET-02). Gap `2026-10-13-network-sync.md` (NET-02).

## Scope
The server-side check for two client-to-server packets that change ship owner and ship attributes (`CmdChOwner`, `CmdShipAttr`), and the two `/ship` commands that call the same logic. Verified by unit tests and by an automated dedicated-server run.

Not covered:
- The other `EntityHelper.checkOP` callers (`C2SGUIPackets.java:432`, `KaitaiHammer.java:72`, `BlockCrane.java:112`). They keep the creative shortcut. Follow-up under NET-01 / NET-07.
- Multiplayer with a human client; the run used one harness player.
- Commit state: the change is uncommitted (not yet committed).

## Evidence
1. Code (working tree; the parent supplied these line references):
   - `src/main/java/com/lulan/shincolle/network/PacketAuthorization.java` (new): `mayEditShips(sender, Predicate)` and `mayEditShips(EntityPlayerMP)`. Both use `getPlayerList().canSendCommands(profile)` with no creative shortcut.
   - `src/main/java/com/lulan/shincolle/network/C2SInputPackets.java`: the `CmdChOwner` case (about line 129, before the `player` reassignment) and the `CmdShipAttr` case (about line 152) `break` when the sender is unauthorized. The sender comes from `ctx.getServerHandler().player`.
   - `src/main/java/com/lulan/shincolle/command/ShipCmdChangeShipOwner.java:124` and `ShipCmdShipAttrs.java:96` now use `mayEditShips`, so a creative non-OP player gets `chat.shincolle:command.notop`. These commands are player-only, as before.
   - `EntityHelper.checkOP` is unchanged.
2. Unit tests: `src/test/java/com/lulan/shincolle/network/PacketAuthorizationTest.java` (3 tests).
   - Red: `docs/porting/reports/logs/2026-10-13-issue2-red.log` (2 failed).
   - Green: `docs/porting/reports/logs/2026-10-13-issue2-green.log`.
   - Command change: `docs/porting/reports/logs/2026-10-13-issue2-ship-cmd-green.log`. No separate red log; it is a one-line swap to the tested predicate.
   - Independent verifier: `test --rerun-tasks` (11 tests, 0 failures) and `build`, both exit 0. All 4 acceptance criteria PASS (as reported by the verifier; its logs are not in this set).
3. Dedicated-server run (automated harness):
   - Dedicated server on 127.0.0.1:25621, RCON on 25631, offline mode, in `run/server-gametest`.
   - Results: `docs/porting/reports/logs/2026-10-13-gametest-dedicated-final-results.txt`; server log: `docs/porting/reports/logs/2026-10-13-gametest-dedicated-final-server.log` (`notop` appears once).
   - All 8 steps PASS: 4 acceptance cases and 4 setup/teardown steps.

     | Step | Result (from results file) |
     |---|---|
     | dedi-connect | joined as Developer, 18 mods (setup) |
     | dedi-rcon-setup | RCON ok (setup) |
     | dedi-spawn-ship-with-egg | egg ship owned by UID 100, then set to foreign owner 9999 by shortcut (setup) |
     | dedi-nonop-survival-forged-packets | non-OP, survival: ownerUID 9999, level 1, bonus 0s, unchanged (acceptance) |
     | dedi-nonop-creative-commands | non-OP, creative: ownerUID 9999, level 1, bonus 0s, unchanged (acceptance; `notop` in server log) |
     | dedi-op-commands | OP, survival: ownerUID 9999 → 100, level 1 → 42, bonus 10..15 (acceptance) |
     | dedi-op-forged-packets-control | OP, forged packets (control): ownerUID 9999 → 100, level 77, bonus 9×6 (acceptance) |
     | dedi-stop-server | stop through the server-thread `/gtprobe stop` (teardown) |

4. Harness workarounds needed for the run:
   - `server.properties`: `spawn-animals=true` and `spawn-npcs=true`. With either set to false, every ship would be deleted (see gap ENT-13, `2026-10-13-entity-lifecycle.md`).
   - Eight throwaway entities are created before the player joins, so the player's entity id is not 0 (see gap NET-13, `2026-10-13-network-sync.md`).

## Confirmed findings
| Feature | Status | Confidence | Historical | Current 1.12.2 | Desired | Evidence |
|---|---|---|---|---|---|---|
| Non-OP player's forged owner/attribute packets are rejected on a dedicated server (survival) | CONFIRMED_WORKING | MEDIUM (one dedicated-server run, offline mode, harness-forged packets, one owner) | Pre-fix: no server OP check (NET-02, inherited) | Rejected after the change; state unchanged | Same | 1, 3 |
| Non-OP creative player cannot use `/shipchangeowner` or `/shipattrs` | CONFIRMED_WORKING | MEDIUM (one run; `notop` message appears once in the server log) | Creative shortcut in `checkOP` allowed it | Blocked with `command.notop`; state unchanged | Same, per the behavior change below | 1, 3 |
| OP player's owner and attribute commands take effect on a dedicated server | CONFIRMED_WORKING | MEDIUM | n/a | owner 9999 → 100, level 1 → 42, bonus 10..15 | Same | 3 |
| OP player's forged packets take effect (control for the packet path) | CONFIRMED_WORKING | MEDIUM | n/a | owner 9999 → 100, level 77, bonus 9×6 | Same | 3 |

The evidence is one automated dedicated-server run, not manual multiplayer.

## Behavior change
- Creative non-OP players can no longer use `/shipchangeowner` or `/shipattrs`, or the owner and attribute packets. This is intended by the fix (NET-02). It is a deliberate change from the creative shortcut in `EntityHelper.checkOP`.
- Rejections at the packet level are silent (no message to the client).

## Assumptions
- The dedicated-server run represents the packet path used by a real client. The harness forged the packets directly.
- `canSendCommands` (vanilla OP level check) is the intended OP criterion. Not checked against 8bca563.

## Unknowns
- Manual multiplayer behavior (a real client, not forged packets): not run.
- Whether other `checkOP` callers (see Scope) need the same change. Follow-up.
- Whether a silent rejection should give feedback to the client. Not decided.

## Risks
- Behavior change for creative non-OP players (see above). Intended by the fix, but it is a player-facing change; the maintainer should confirm.
- The harness needs `spawn-animals=true` and `spawn-npcs=true`. A dedicated-server test with either set to false would delete all ships (ENT-13).
- The harness workaround for entity id 0 hides NET-13 (the first player to join gets id 0 and is rejected).
- Packet-level changes change the server-side behavior but not the wire format. No registry ID, persistent data or build change is part of this report.

## Recommended next actions
1. Human review of the behavior change for creative non-OP players. Needs human approval.
2. Decide whether to apply NET-13's proposed `>= 0` change, so that the first player to join is not rejected. Needs approval; not applied or tested.
3. Follow-up under NET-01 / NET-07 for the other `checkOP` callers. Needs approval to scope.
4. Commit only when asked. The change is currently uncommitted.
