# runClient Smoke Test (Cleanroom, Java 25)

- Date: 2026-10-13
- Phase: 1
- Author: Claude Code
- Related issue/ADR: ADR-0001

## Scope
Launch the dev client with `./gradlew runClient` after the buildscript migration and check that the mod loads. In-game behavior (world, entities, GUIs) was NOT tested.

## Evidence
Log: `logs/2026-10-13-runClient-1.log` (342 lines).
1. Runtime: OpenJDK 25.0.4 (Temurin), Windows, Cleanroom 0.6.13-alpha initialized (log lines 67-68, 127).
2. FML reports "successfully loaded 17 mods", including `shincolle`, `mekanism` (+generators, tools), `mantle`, `theoneprobe`, `mixinbooter`.
3. ShinColle logged Pre-Init, Init and Post-Init completed with no exception (log lines 246, 324, 333).
4. The client process kept running after Post-Init (java.exe still present when checked). Whether the main menu rendered was not observed by the agent.
5. 8 ERROR lines: 3 about Maven library folder format and 1 about missing binary patch set (both dev-environment noise from Cleanroom), 4 `MultiModel minecraft:builtin/missing is empty`.
6. 6 ShinColle WARN lines about missing sound files: `feed3301.ogg`, `feed3302.ogg`, `hurt54013.ogg` (sound events `ship-feed-33`, `ship-hurt-54`) and missing sound for events `ship-idle-60`, `ship-hit-60`, `ship-hit-62`.

## Confirmed findings
| Feature | Status | Confidence | Historical | Current 1.12.2 | Desired | Evidence |
|---|---|---|---|---|---|---|
| Mod loads under Cleanroom on Java 25 (init lifecycle) | CONFIRMED_WORKING | MEDIUM | n/a | Pre/Init/Post-Init complete | n/a | 1-3 |
| Sound events for ship 33, 54, 60, 62 | CONFIRMED_BUG | LOW | unknown | referenced files missing from resources | unknown | 6 |

The sound row is a hint only: whether these files existed upstream in 1.10 is not yet researched (see Unknowns). Not fixed, per the no-unrelated-fixes rule.

## Assumptions
- The 4 `builtin/missing` errors come from the dev environment or missing model, not ShinColle specifically.

## Unknowns
- Whether the main menu rendered and whether the game is playable.
- Whether the missing sounds also lack entries upstream in 1.10.
- Behavior of entities, GUIs, TESR/JSON models, and Mekanism/TOP integration in-world.
- Whether `builtin/missing` errors are caused by ShinColle models.

## Risks
- Registry and NBT behavior is untested; a loaded mod is not proof of working features.

## Recommended next actions
1. Human launches the same client (`JAVA_HOME=~/.jdks/temurin-25.0.4 ./gradlew runClient`), reaches a world, and reports observations; record them here.
2. Research the missing sound files against upstream 1.10 (`historical-researcher`).
3. Continue the feature inventory.
