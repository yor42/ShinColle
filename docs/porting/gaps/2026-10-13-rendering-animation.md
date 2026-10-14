# Gap Analysis: Rendering and Animation (RND-)

- Date: 2026-10-13
- Phase: 2
- Author: Claude Code (legacy-code-analyst subagent, compiled by main session)
- Related issue/ADR: backlog `2026-10-13-gap-backlog.md`; smoke log `../baseline/logs/2026-10-13-runClient-1.log`

## Scope
`client/**`, `ClientProxy`, block and item model registration, `assets/shincolle/{models,blockstates,sounds,sounds.json,textures}`, the Metamorph hooks in `EventHandler`, and the smoke log. Static analysis plus resource-consistency scripts that the subagent ran read-only. Nothing was run in-world.

## Evidence
Automated static checks:

| Check | Result |
|---|---|
| Every `shincolle:` texture or parent in 111 model JSONs, and every blockstate `model` | 0 missing |
| Every `Tags.TEXTURES_*+"X.png"` literal in Java (92) | 0 missing |
| Every registered entity has a renderer | yes (only `EntityElephant` and `EntityLion` mismatch, and they appear only in comments) |
| Item model files vs `getTypes`/`getIconTypes` | consistent |
| `sounds.json` (114 events) vs `.ogg` files | 3 missing files; 19 unreferenced `.ogg` |
| Asset diff 8bca563 → HEAD | Only `blockdesk.png` and `blocksmallshipyard{off,on}.png` were removed (replaced by JSON models). No model or texture was lost. |

The face, emotion and pose state machine (`ShipModelBaseAdv.setFaceNormal/Cry/Attack`, `setFlush`) is unchanged from 8bca563 apart from formatting.

## Confirmed findings

| ID | Feature | Status | Conf. | Historical (8bca563) | Current (HEAD) | Desired (proposal) | Evidence |
|---|---|---|---|---|---|---|---|
| RND-01 | `ship-feed-33` references nonexistent `feed3301/3302.ogg` | CONFIRMED_BUG | HIGH | The event did not exist | `sounds.json:411-415`; the files never existed in any commit. Introduced by 3338196 (misaka10843, 2022). | Add the files or remove the entry | log 281-282 |
| RND-02 | Typo `hurt54013` (file is `hurt5403.ogg`) | CONFIRMED_BUG | HIGH | `ship-hurt-54` = `hurt5401` only | `sounds.json:553-558` (3338196) | Reference `hurt5403` | log 283 |
| RND-03 | Events `ship-idle-60`, `ship-hit-60`, `ship-hit-62` were dropped from `sounds.json`, but config still registers them | CONFIRMED_BUG (the removal may be deliberate: UNKNOWN_INTENT) | HIGH | `sounds.json:370-383` defined them (`pan1/2`, `burnlove1/2`) | The default `ConfigSound.java:86-87` rates register the events (`ModSounds.java:158-175`). `getCustomSound` (`BasicEntityShip.java:220-241`) then returns a silent event 25-50% of the time. The `.ogg` files still exist, unreferenced. Removed by 3338196. | Restore the entries or change the default config | log 284-286 |
| RND-04 | 4× `builtin/missing is empty` at model bake | UNKNOWN_INTENT | LOW | Same blockstates | `blocklightair.json` and `blocklightliquid.json` have empty variants (2 blocks × 2 = 4, which matches the count) | Silence or accept | log 318-321 (attribution assumed) |
| RND-05 | TESR → JSON migration leftovers | UNKNOWN_INTENT (candidate PLANNED_DEVIATION, b8cdf42) | MEDIUM | TESRs for Desk, SmallShipyard, LargeShipyard | `RenderTileEntityItem.java:25-56` still replaces `TileEntityItemStackRenderer.instance` (`ClientProxy.java:86`) with likely unreachable branches. `ModelBlockDesk`, `ModelSmallShipyard` and `custommodelitemblock.json` are unreferenced. `RenderLargeShipyard` remains a TESR, bound in `BlockGrudgeHeavy.java:97`. | Verify in-world, then remove dead code (needs approval) | static |
| RND-06 | Waypoint render (new) | UNKNOWN_INTENT (candidate PLANNED_DEVIATION: e6b9edc, 0892269, fbf4efe) | MEDIUM | `getRenderType` INVISIBLE; no renderer | A TESR is shown only when the player holds the wrench or waypoint block and is creative, OP or owner (`RenderWaypoint.java:31-40`). It is registered from `BlockGrudgeHeavy.initModel` (`:98`), an unrelated block. | Confirm intent | static |
| RND-07 | Metamorph phantom ships; ghost HP code | CONFIRMED_BUG (`==` string compare) / UNKNOWN_INTENT (phantom cause) | HIGH / LOW | Same (`==` at 8bca563 `EventHandler` :1289) | `name == "generic.maxHealth"` on an NBT string (`EventHandler.java:~1128`) is effectively dead. Ghost suppression covers only `BasicEntityShip`. Metamorph has never been loaded in any run. | Reproduce with Metamorph | static |
| RND-08 | Model stubs (DestroyerHa/I/Ni/Ro, Rensouhou) | UNKNOWN_INTENT (inherited) | LOW | 4 stubs per model | Empty `showEquip`, `syncRotationGlowPart`, `applyDeadPose`, `applyNormalPose` | Not established | static |
| RND-09 | Guide book picture page; desk chapters 4-5 text | UNKNOWN_INTENT | LOW | Same TODO (`GuiBook.java:316`) | `GuiBook.java:302` TODO. `GuiDesk.java:356-371` hides text when `shipModel==null` (uncollected ship). The functional part of 68027fd ("图鉴不显示" fix) is buried in reformatting. | Not established | static |
| RND-10 | Erebus item rendering (Bugs.txt) | UNKNOWN_INTENT | LOW | Not established | Hypothesis: the `TileEntityItemStackRenderer.instance` swap (2a7e30a, misaka10843) | Not established | Bugs.txt |
| RND-11 | Mixed-case ResourceLocation literals | UNKNOWN_INTENT | LOW | Same literals; files lowercased by Theray070696 | E.g. `RenderShipEntity.java:28`, `ClientProxy.java:28`. This works only if 1.12 lowercases paths. | Lowercase-only convention on the Modern track (renaming needs approval) | static |

### Per-record details

| ID | Reproduction | Severity | Depends on | Stabilize before Modern? | Candidate regression tests |
|---|---|---|---|---|---|
| RND-01 | `runClient`, read SoundHandler WARN lines | LOW | `ConfigSound` | no | Every `sounds.json` entry has a file |
| RND-02 | same | LOW | same | no | same, plus every `.ogg` is referenced |
| RND-03 | Ship class 58 or 60 (keys 60 and 62) with default config: idle and hit sounds are sometimes silent | MEDIUM | `ConfigSound`, `ModSounds` | yes (cheap data consistency) | Every `ConfigSound` default key has a `sounds.json` event |
| RND-04 | `runClient`, grep the log | LOW | none | no | Flag empty blockstate variants |
| RND-05 | Place desk, small shipyard (on/off), large shipyard (formed/unformed); check icons, models and facing | MEDIUM until verified | `BasicBlockFacing` | partial (visual check only) | Model-resolution script in CI |
| RND-06 | Place a waypoint; barehanded (invisible) vs holding the wrench | LOW | `TargetWrench` | no | none |
| RND-07 | Add metamorph and mclib to the run config (build change, needs approval); morph, demorph or log out; look for ghosts | MEDIUM | MOD-01, MOD-02 | no (Metamorph scope is undecided) | `equals` after extraction |
| RND-08 | Kill or sit those ships | LOW | none | no | none |
| RND-09 | Desk book chapters 4 and 5 with collected and uncollected ships | LOW | `CapaTeitoku` | no | none |
| RND-10 | Run with The Erebus (not available) | LOW | RND-05 | no | none |
| RND-11 | n/a | LOW now; a hard failure on the Modern track | none | no | Lowercase-literal scan |

## Assumptions
- 1.12.2 `ResourceLocation` lowercases paths (RND-11). Not verified.
- The 4 `builtin/missing` errors come from the light blocks (RND-04).
- The `RenderTileEntityItem` branches are unreachable (RND-05). Not run.

## Unknowns
- Whether the main menu, world, ship models, GUIs, HUD and TESRs render correctly. The smoke run never reached a world.
- Metamorph, Baubles and Erebus behavior (never loaded).
- Whether the unreferenced `.ogg` files are intentional leftovers.

## Risks
- RND-03 is a likely user-visible regression and is cheap to confirm.
- The `TileEntityItemStackRenderer.instance` swap can interact unpredictably with other mods.
- Uppercase resource literals will break on the Modern track.

## Minecraft-independent logic candidates
Emotion and face selection arithmetic (not the `ModelRenderer` calls); `ModSounds.getCustomSoundString` and `ConfigSound` parsing; `Values.BookList` and book paging math; leash height and shadow-size tables; the ship class → texture/model mapping; particle ID → parameter mapping.

## Recommended next actions
1. Human in-world visual check (blocks, ships, GUIs, HUD).
2. Maintainer decision on RND-01, RND-02 and RND-03 (data-only fixes).
3. A resource-consistency JUnit test once `src/test` exists.
