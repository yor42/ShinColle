# Prioritized Gap Backlog (Phase 2)

- Date: 2026-10-13
- Phase: 2
- Author: Claude Code
- Related issue/ADR: none; GitHub issues are proposed below and have not been created

## Scope
This document merges the eight subsystem gap reports in this directory into one prioritized backlog:

| Prefix | Report |
|---|---|
| ENT | `2026-10-13-entity-lifecycle.md` |
| ATTR | `2026-10-13-attributes-progression.md` |
| CMB | `2026-10-13-combat-equipment.md` |
| AI | `2026-10-13-ai-pathfinding.md` |
| INV | `2026-10-13-inventory-persistence.md` |
| RND | `2026-10-13-rendering-animation.md` |
| NET | `2026-10-13-network-sync.md` |
| MOD | `2026-10-13-mod-integrations.md` |

The historical baseline is `8bca563` (see `../baseline/2026-10-13-historical-lineage.md`).

**Evidence basis for every entry is static (code reading and diff).** No game was launched into a world and no test exists. Following `feature-status.md`, CONFIRMED_BUG here means "deterministic from code, with an intended-behavior source". Each one still needs a reproduction (test or manual check) as the first step of its Phase 3 task. **Nothing is CONFIRMED_WORKING.**

Full per-record fields are in each subsystem report: description, current, historical and desired behavior, evidence, reproduction, severity, confidence, dependencies, Modern-port stabilization flag, and candidate tests.

## Evidence
Spot-verified by the main session by re-reading the cited lines: ATTR-03, ATTR-08, NET-02, NET-04/ATTR-01, ENT-02, ENT-08, CMB-01, CMB-02, AI-01, AI-03, INV-03. The rest rely on subagent reading and diffs.

## Origin legend
- **PORT**: regression introduced by the 1.12.2 port commit `7b71860`.
- **FORK**: regression introduced by a later fork commit.
- **INH**: inherited unchanged from `8bca563`.
- **NEW**: fork-added feature with no historical counterpart.

## Prioritized backlog

### P0: regressions with clear intended behavior; crash, data loss, duplication, or broken core loop
Stabilization is required before the Modern track for every P0 item.

| Rank | ID | Summary | Origin | Severity | Conf. | Test seam |
|---|---|---|---|---|---|---|
| 1 | ATTR-03 | `InventoryHelper.getAndRemoveItem` returns the full stack while removing `minsize`: **item duplication** (crafting task) | PORT 7b71860 | HIGH | HIGH ✔ | stack-take arithmetic |
| 2 | AI-01 | Guard-mode target box `grow(+r)` then `grow(-r)`: guard ships cannot find targets | FORK 7296046 | HIGH | HIGH ✔ | pure AABB function |
| 3 | NET-04 / ATTR-01 / ATTR-02 | `MetamorphHelper.decrSupplies` polarity inverted: free supplies, items eaten, Repair Goddess inverted (Bugs.txt line 10) | PORT 7b71860 | HIGH (Metamorph users) | HIGH ✔ | consume-decision function |
| 4 | ENT-08 | Owner sync radius 64 blocks became 8 (`getDistanceSq < 64`) | PORT 7b71860 | MEDIUM | HIGH ✔ | radius helper |
| 5 | AI-03 | `checkDepth` +1 over-count and 8-tick staleness: float behavior for every ship | FORK 57b124b | HIGH | HIGH ✔ (effect MEDIUM) | depth over a column |
| 6 | INV-03 | Ship NBT load null guards removed: NPE on partial NBT | FORK d7349f8 | MEDIUM | HIGH ✔ | ship-state DTO |
| 7 | AI-02 | Stationary guard fire cadence polled every 32 ticks | FORK 7296046 | MEDIUM | MEDIUM | attack-timer model |
| 8 | ENT-02 | `Death_Time=0` → no egg, ship lost; first 10 ticks no drop | INH | HIGH (config) / LOW | HIGH ✔ | `shouldDropEgg` |

### P1: multiplayer trust and persistence (inherited, or new with clear intent)

| Rank | ID | Summary | Origin | Severity | Conf. |
|---|---|---|---|---|---|
| 9 | NET-02 | `CmdChOwner`/`CmdShipAttr` packets have no server OP check | INH | CRITICAL (public servers) | HIGH ✔ |
| 10 | NET-01 | C2S actor taken from packet fields; no target ownership checks | INH | HIGH (MP) | HIGH |
| 11 | NET-03 | Unbounded client-controlled array and string lengths | INH? | HIGH (MP) | MEDIUM |
| 12 | NET-07 / NET-06 / INV-04 | Tile GUI, waypoint pairing and inventory page packets lack reach and owner validation | INH | MEDIUM | MEDIUM-HIGH |
| 13 | INV-01 | Crane FE/EU capacity loads as 0 on pre-FE saves | NEW (914f4b9) | MEDIUM | HIGH |
| 14 | ENT-03 | Ship Spawn Egg consumed (with XP) before spawn; no refund on failure | INH | MEDIUM | HIGH |
| 15 | INV-07 | Egg with a tag but no `Attrs` throws on right-click | INH | MEDIUM | MEDIUM-HIGH |
| 16 | MOD-01 / NET-11 | Unguarded `MetamorphHelper` calls: possible `NoClassDefFoundError` without Metamorph (needs a launch test) | INH | HIGH if real | LOW |
| 17 | INV-05 | More than 2 drums → out-of-range exception in Crane loops (reachability unknown) | NEW | HIGH if reachable | MEDIUM |
| 18 | NET-05 | `toBytes` reads and mutates live state (`CmdShipList`, `TeamData`) | INH | HIGH/MEDIUM | MEDIUM |
| 19 | RND-03 | Sound events for keys 60 and 62 lost from `sounds.json`: silent idle and hit cues | FORK 3338196 | MEDIUM | HIGH |
| 20 | RND-01 / RND-02 | Missing `feed3301/3302` files; `hurt54013` typo | FORK 3338196 | LOW | HIGH |
| 21 | INV-02 | `Tile_Crane` config resize resets user values | NEW | LOW | HIGH |

### P2: inherited gameplay bugs that need a balance decision (fix or PLANNED_DEVIATION)

| ID | Summary | Origin | Severity |
|---|---|---|---|
| CMB-01 | Triple hit (x3) unreachable: duplicate `roll <= dhit` | INH | MEDIUM |
| CMB-02 | Day/night damage tables swapped | INH | MEDIUM |
| CMB-03 | Resistance potion vs missiles unreachable | INH | LOW |
| CMB-06 | Dodge clamp ignores "-1 = no limit" | INH | LOW |
| ATTR-06 | Morph Training Book ignores the marriage cap | INH | MEDIUM |
| ATTR-07 | `ExpNext` stale after book or with custom `expMod` | INH | LOW |
| ATTR-08 | Fishing luck level bonus integer division (0 below 150) | INH | LOW |
| AI-04 | `yOffset` uses x; stuck-recovery sign | INH | MEDIUM/LOW |
| NET-08 / NET-10 / NET-12 | Codec asymmetries, unchecked casts, dead sync call | INH | LOW |
| RND-07 | `==` string compare makes the morph ghost HP code dead | INH | LOW |

### P3: UNKNOWN_INTENT and candidate PLANNED_DEVIATION; routed to the maintainer (see questions below)
ENT-04, ENT-05, ENT-07, ENT-11, ENT-12, ATTR-04 (bowl return), ATTR-05 (57b124b), ATTR-10, CMB-04, CMB-05, CMB-07, CMB-08, CMB-09, AI-05, AI-06, AI-07, AI-08, INV-06, INV-08, INV-09, INV-10, INV-11, RND-04, RND-05, RND-06, RND-08, RND-09, RND-10, RND-11, NET-09, MOD-02 … MOD-12.

### Features examined with no defect found (no status assigned; not CONFIRMED_WORKING)
Entity registration (87 entries identical), despawn rules, airplane and summon lifetime, mount lifecycle, the stat pipeline formulas, Modernization and marriage logic, morale, fuel and ammo, AI task registration (135 entries identical), pathfinder core, Baubles guard, asset and model resolution (0 missing textures or models), the face and emotion state machine, packet main-thread scheduling, and NBT key layout (no ship, player, egg or world key changed).

## Recommended first three stabilization tasks

Selection criteria: verified by re-reading; a regression with an unambiguous intended behavior (no balance decision needed); high player impact; a small, local fix; a testable seam that can also bootstrap `src/test`.

1. **ATTR-03: stop item duplication in `InventoryHelper.getAndRemoveItem`.** Two branches use `grow(minsize)` where 8bca563 used `stackSize = minsize`.
   - Test first: a JUnit characterization test of the take-from-stack arithmetic, which also establishes `src/test`. If `ItemStack` needs a Minecraft bootstrap under Unimined, extract the count math.
   - Then a manual crafting-task check.
2. **AI-01 (+ AI-02 if the cadence is decided): restore the guard-mode target search box.**
   - Test first: a pure test that the search box contains the ship box extended by 0.9r/0.6r. `AxisAlignedBB` is a plain value class.
   - Then a manual guard-block combat check.
   - AI-02 joins only if the maintainer confirms the per-tick cadence.
3. **NET-04 / ATTR-01 / ATTR-02: fix the `decrSupplies` polarity.** One line fixes three symptoms and the Bugs.txt entry.
   - Test first: the consume decision as a pure function.
   - Then a manual check with Metamorph loaded. Adding Metamorph to the dev runtime needs approval as a dependency-scope change. If it is not approved, verify through the unit test plus a later manual run.

Next batch candidates: ENT-08, AI-03 (needs a runtime trace because it changes float feel), INV-03, and NET-02 (promote to the first three if public multiplayer servers are in scope).

**Prerequisite (Phase 3 task 0, proposed):** a runtime smoke test that reaches a world (spawn a ship, open its GUI, feed it, save and reload). The baseline smoke run never entered a world, so every in-world check above depends on this.

## GitHub issues (created 2026-10-13 after approval, decision P-18)
Created as #1-#8 with labels `bug`, `ready-for-agent`, in the order below. Decisions from the grilling session (P-7 to P-19, `../decisions/README.md`) supersede the open questions and the "recommended first three" above: the Phase 3 first batch is #1 → #2 → #3 → #4 (P-13).

Original proposal:
Label `needs-triage` unless the maintainer decides otherwise:
1. "Item duplication: getAndRemoveItem returns full stack (crafting task)" (ATTR-03)
2. "Guard mode ships cannot find targets: search box cancels itself" (AI-01, AI-02)
3. "Morph supply buttons and Repair Goddess have inverted item check" (NET-04, ATTR-01, ATTR-02)
4. "Owner sync radius reduced from 64 to 8 blocks" (ENT-08)
5. "Ship depth over-counted by 1 after depth optimization" (AI-03)
6. "Ship NBT load crashes on missing sections" (INV-03)
7. "Server does not verify OP for ship owner/attribute command packets" (NET-02; title kept generic; details only in a private channel if preferred)
8. "Ship sounds for classes 58/60 silent; sounds.json entries missing" (RND-01, RND-02, RND-03)

## Assumptions
- Severity assumes default config and a single-player or small trusted server, unless noted.
- "PORT" and "FORK" attribution comes from `git log -S` and diffs, not from a bisect with runtime tests.

## Unknowns
- Whether the game reaches a world and whether ships spawn, move and fight at all under Cleanroom on Java 25. This is the largest unknown and it gates every runtime check.
- Optional-mod behavior (Metamorph, Baubles, IC2, TiC): none was loaded at runtime.

## Risks
- Fixing P2 items changes gameplay balance players have lived with since 1.10, so they need explicit decisions.
- P1 multiplayer fixes may change the wire protocol (client and server must match), not persistent data.
- Any fix touching NBT load must keep the key layout. Persistent-format changes need approval.

## Recommended next actions
1. Maintainer resolves the decision questions in the Phase 2 gate report (suggested via the grilling workflow).
2. Approve the P0 order, the first three tasks, PLANNED_DEVIATION designations, and the Phase 3 exit criteria.
3. Approve or reject the creation of the proposed GitHub issues.
