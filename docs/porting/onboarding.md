# Build, test and run onboarding

One page for a new contributor or agent. Facts below were read from `build.gradle`, `docs/adr/0001-java-25-cleanroom-only.md` and `src/`; commands are not claimed as run unless a report says so.

## Target
- Java 25 and Cleanroom only (ADR-0001, `docs/adr/0001-java-25-cleanroom-only.md`). Plain Forge and Java 8 are not supported.
- Gradle 9.8.0 itself must run on JDK 25. Set `JAVA_HOME` to a JDK 25 install.
- Project phase, gates and directory map: `docs/porting/README.md`. Glossary: `CONTEXT.md`.

## Commands (PowerShell)
```
$env:JAVA_HOME = "<path to JDK 25>"
./gradlew.bat build          # compile, tests, jar
./gradlew.bat test --rerun   # unit tests only, forces re-execution
./gradlew.bat runClient      # dev client (needs a desktop)
./gradlew.bat runServer      # dev dedicated server
```
Run one Gradle process per checkout. Do not start a second Gradle or client process against the same checkout or dev world.

## Source sets
| Source set | Path | Purpose |
|---|---|---|
| main | `src/main/java`, `src/main/resources` | the mod |
| test | `src/test/java` | JUnit 5 (`junit-jupiter`); classpath copies main's (`build.gradle`, `test {}` block) |
| gametest | `src/gametest/java` | dev-only in-game harness, never in the jar; see `docs/porting/gametest-howto.md` |

## What `test` proves
- `test` runs only `src/test`. Output "NO-SOURCE" or 0 tests verifies nothing.
- Unit tests that touch vanilla classes call `net.minecraft.init.Bootstrap.register()` first (see `TestEnvironmentTest`).
- Optional-mod API classes (for example IC2) are compile-only and are missing at test runtime; a class that implements them cannot be loaded in a unit test (found while writing the #70 packet tests: `TileEntityCrane`).
- A successful compile, a passing unit test or a client launch is not proof of in-world behavior. Use the gametest harness or a recorded manual check.
- Mods for dev runs live in `debugMod/` (flatDir) and use `mod*` configurations (`gradle/scripts/dependencies.gradle`).
