# Gap Analysis: Combat and Equipment Calculations (CMB-)

- Date: 2026-10-13
- Phase: 2
- Author: Claude Code (legacy-code-analyst subagent; CMB-01 and CMB-02 spot-verified by the main session)
- Related issue/ADR: backlog `2026-10-13-gap-backlog.md`

## Scope
The damage paths of `utility/{CombatHelper,CalcHelper,BuffHelper,TeamHelper}` and `EntityHelper`; the `attackEntityFrom` pipelines of ships, hostile ships, mounts, summons and airplanes; `EntityAbyssMissile`, `EntityProjectileBeam`, `EntityFloatingFort`; `EntityBattleshipNGT`; `crafting/EquipCalc`; `item/Equip*`; and the tables in `Values`, `ID` and `Attrs*`. Static analysis plus a whitespace-normalized diff against `8bca563`. Nothing was run. AI targeting and the misaka10843 "Y-axis angle" hit fix are covered by the AI report.

**Key result:** this subsystem is behaviorally near-identical to 8bca563 (API renames, `.isEmpty()`, switch syntax). Every significant defect below is **inherited from upstream**, not a port regression. The 1.10→1.12 `AxisAlignedBB.expand` semantics trap was already fixed by `1d9f4a2` (KCFindstr: `grow`); no `.expand(` remains.

## Evidence
1. Main-session verification: `CombatHelper.java:101` and `:106` both test `canMultiHit && roll <= dhit`. The second branch (x3, triple hit) is unreachable, and `thit` (computed at `:83-85`) is never read. The same appears in 8bca563.
2. Main-session verification: `CombatHelper.java:202-203` has `modDay = Values.ModDmgNight[...]` and `modNight = Values.ModDmgDay[...]`. `Values.java:575` labels `ModDmgDay` "for day battle" and `:584` labels `ModDmgNight` "for night battle". `lightCoef` 1 = day (`BuffHelper.java:516-526`).
3. Missile damage sources are built with `causeMobDamage(host)` (`EntityAbyssMissile.java:572`, `EntityFloatingFort.java:179`), so `getImmediateSource()` is the ship, never the missile.
4. TODO/NYI in scope: `CombatHelper.java:216` (also in 8bca563), `BuffHelper.java:553`, `MulitBlockHelper.java:26` (NYI Large Workshop), `EquipDrum.java:112` (NYI EU tooltip).

## Confirmed findings

| ID | Feature | Status | Conf. | Historical (8bca563) | Current (HEAD) | Desired (proposal) | Evidence |
|---|---|---|---|---|---|---|---|
| CMB-01 | Triple hit (x3) never triggers | CONFIRMED_BUG (inherited) | HIGH | Same duplicate condition | The THIT stat from equipment, potions and buffs is dead (Ev. 1) | The third branch tests `roll <= thit`, per the documented priority miss > cri > dhit > thit (javadoc `:46-58`). Maintainer to confirm x3 is wanted. | Ev. 1 (verified) |
| CMB-02 | Day/night damage modifiers swapped | CONFIRMED_BUG (inherited) | MEDIUM | Same swap | At midday the night table applies, and the day table at night. The aircraft row (day {1.5…2} vs night {0.5…1}) is inverted, so carriers are weak by day. | Day uses `ModDmgDay` | Ev. 2 (verified); intent cannot be proven, so it needs confirmation |
| CMB-03 | Resistance potion (11) vs missiles is unreachable | CONFIRMED_BUG (inherited) | MEDIUM | Same | `BuffHelper.java:484` checks `getImmediateSource() instanceof EntityAbyssMissile`, which is never true (Ev. 3). Fire Resistance (12) covers missiles instead. | Identify missiles by the explosion flag or a dedicated source | static |
| CMB-04 | Enchant gating uses `EQUIP_TYPE`, not `ENCH_TYPE`; aircraft-ATK branch | UNKNOWN_INTENT (aircraft branch: candidate PLANNED_DEVIATION, 7a0ee73 "Apply enchantment on aircraft") | MEDIUM | `equipType == 1` only | `EquipCalc.java:258,312-321,368-380`. `ENCH_TYPE` is used only for the tooltip (`BasicEquip.java:193-197`). Enchant IDs are numeric (`EnchantHelper.java:74-110`), which is a Modern-port risk. | Not established | static, diff |
| CMB-05 | Inconsistent damage pipeline per attack kind | UNKNOWN_INTENT (inherited) | MEDIUM | Same | Melee skips combat rolls, player reduction and friendly-fire checks (`BasicEntityShip.java:2107-2143`). Heavy attacks roll only miss. Missiles have no multi-hit. Summons skip resist potions. Stale TODO at `CombatHelper.java:209-217`. | Not established | static |
| CMB-06 | Submarine dodge clamp with limit -1 | CONFIRMED_BUG (latent, config-dependent) | MEDIUM | Same | `CombatHelper.java:160-161` clamps without the `>= 0` guard that `Attrs.checkAttrsLimit` uses, so `-1` disables dodge | Clamp only when limit >= 0 | static |
| CMB-07 | 18.11-inch / 46cm Triple Cannon (meta 16) | UNKNOWN_INTENT (fork addition dbd6499, 00b8ecd) | LOW | Absent | `Values.java:620`: SPD 0.1, DHIT 0, THIT 0.2 (dead, CMB-01), unlike peer cannons | Not established | diff |
| CMB-08 | Nagato Armor-Piercing Fist AoE | UNKNOWN_INTENT (inherited) | LOW | Same | `atk2 = atk1*0.5` against a javadoc that says 1x. The primary target is hit twice, and ammo is spent on every charge phase (`EntityBattleshipNGT.java:175-309`). | Not established | static |
| CMB-09 | Hostile attribute limits; potion documentation drift | UNKNOWN_INTENT (inherited) | LOW | Same | Hostile ships skip config limits (`BuffHelper.java:876-901`). Nausea adds a flat +0.4 miss against a documented -0.4/lv (`CombatHelper.java:142`). Water breathing ASM is +20/lv against a documented +60. | Not established | static |

### Per-record details

| ID | Reproduction | Severity | Depends on | Stabilize before Modern? | Candidate regression tests |
|---|---|---|---|---|---|
| CMB-01 | THIT 0.9, DHIT 0, many light shots: no x3 ever | MEDIUM | CMB-07 | yes: decide, then pin before extraction | Pure `rollOutcome(roll, miss, cri, dhit, thit, canMulti)` boundary table |
| CMB-02 | A carrier attacks at midday and at midnight; compare with the tables | MEDIUM | night-vision potion | yes: characterize, then decide | `modDamageByLight(dmg, atk, def, light)`; the tables must move out of `Values` (its static init touches `ModItems`, `Values.java:978`) |
| CMB-03 | A ship with Resistance vs Fire Resistance, hit by missiles | LOW | CMB-05 | no (record; damage APIs change in 1.13+) | Resist function over a source-kind enum |
| CMB-04 | Compare the enchant-type tooltip with the stat that changes | LOW | `EnchantHelper` | no; record the numeric-enchant-ID dependency | `calcEquipStatWithEnchant` table (already nearly pure) |
| CMB-05 | n/a (structural) | LOW | CMB-01, CMB-03 | no; keep the ordering when extracting | `modDamageByAdditionAttrs` with plain args |
| CMB-06 | Set the DODGE limit to -1; a submarine beyond 6 blocks | LOW | config | no | `computeDodge(base, isSub, invis, distSq, limit)` at 36, 256 and -1 |
| CMB-07 | n/a | LOW | CMB-01 | no (registry ID stable) | Every `EquipSubID` has `EquipAttrsMain` and `EquipAttrsMisc` entries |
| CMB-08 | n/a | LOW | `TeamHelper` | no | none |
| CMB-09 | n/a | LOW | config | no | `calcAttrsBuffed` table with the `scaleShip` seam |

## Assumptions
- "Inherited" means the same source exists in 8bca563. Whether players of 1.10 builds observed it is not established.
- A miss sends `attackEntityFrom(source, 0F)`. Whether vanilla mobs register a 0-damage hurt (and receive on-hit buffs) is unverified.

## Unknowns
- Numeric audit of the morale and formation tables, `EnchantHelper` values, and `EquipAttrsMain`.
- Line-by-line check of the airplane, Rensouhou and `EntityProjectileStatic` damage paths.
- Whether day/night and triple hit were ever discussed upstream as intentional.

## Risks
- Fixing CMB-01 or CMB-02 changes combat balance noticeably for existing players. These are gameplay changes and need maintainer approval.
- Porting the swapped or dead logic into a shared core would bake the bugs into both tracks. Pin them with characterization tests first.

## Minecraft-independent logic candidates
`applyDamageReduceByDEF`; combat-roll threshold logic and `calcMissRate`; `canDodge`; `modDamageByLight`; `modDamageByAdditionAttrs`; `getAttackDelay`; `BuffHelper.calcAttrsBuffed` and `convertBuffMapToAttrs`; `EquipCalc.calcEquipStatWithEnchant`; `TeamHelper` ally, ban and friendly-fire rules (behind a team-lookup interface); `Values` stat tables (split from the icon map); `CalcHelper` math. These are the strongest shared-core candidates in the mod.

## Recommended next actions
1. Golden-table characterization tests for the pure functions above, before any fix (Phase 3 test harness).
2. Maintainer decisions: CMB-01 and CMB-02 (fix or PLANNED_DEVIATION), CMB-04, CMB-05, CMB-08, CMB-09.
