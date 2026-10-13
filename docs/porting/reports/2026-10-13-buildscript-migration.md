# Buildscript Migration to CleanroomModTemplate-mixin

- Date: 2026-10-13
- Phase: 1 (prerequisite)
- Author: Claude Code
- Related issue/ADR: decisions P-1, P-2, P-5, P-6 in `docs/porting/decisions/README.md`

## Scope
Replace the RetroFuturaGradle buildscript with one based on `C:\Projects\CleanroomModTemplate-mixin`. No gameplay Java or assets were edited. Runtime launch was not tested.

## Evidence
1. Original build, JDK 17, Gradle 8.10.2: FAILED at `build.gradle` line 13; plugin `com.gtnewhorizons.retrofuturagradle:1.4.1` not found (GTNH Maven `jenkins.usrv.eu:8081` unavailable). Log: `baseline/logs/2026-10-13-build-original.log`.
2. First migrated attempt on JDK 17 failed: Unimined 1.4.43-kappa requires JVM 25 (`...-migrated-1.log`).
3. On JDK 25 (`~/.jdks/temurin-25.0.4`): attempt 2 failed on a leftover `helpers.gradle` include; attempt 3 compiled with 70 errors because `files("debugMod/...")` jars did not reach the compile classpath via `modCompileOnly` (Metamorph, Baubles, MCLib). Logs `-2.log`, `-3.log`.
4. Attempt 4 (flatDir repo for `debugMod/`): `./gradlew build` exit 0, BUILD SUCCESSFUL in 35s, 0 compile errors. Log `-4.log`.
5. Output `build/libs/shincolle-1.12.2.1.11.jar` has 553 classes, `Tags.class`, `pack.mcmeta`, and an expanded `mcmod.info` with correct metadata. Manifest has `ModType: CRL`.

TinyRemapper "Invalid access" warning lines were stripped from the saved logs to keep them readable.

## Confirmed findings
- The original buildscript is unusable as of today.
- The migrated build compiles all source files unchanged on Java 25 and produces a remapped jar.
- `test` ran with NO-SOURCE (there are no tests).

## Changes made
- Replaced: `build.gradle`, `settings.gradle`, `gradle/wrapper/*` (Gradle 9.8.0), `gradlew*`, `gradle/scripts/{dependencies,extra,publishing}.gradle`, `gradle.properties` (mod metadata preserved verbatim; added `mod_issue_tracker`, `use_mixins=false`).
- Deleted: `gradle/scripts/helpers.gradle`, `tags.properties` (superseded by `src/main/java-templates/com/lulan/shincolle/Tags.java`, same field names and values).
- Moved: `src/main/resources/mcmod.info` to `src/main/resource-templates/mcmod.info` (placeholder syntax now `{{ }}`).
- Template deviations: mixin remap, manifest `MixinConfigs`, and `crl.dev.mixin` are gated by `use_mixins` (ShinColle has no mixins); the template's example JEI dependency was removed.
- Dependencies, same versions as before: Mekanism, Mantle, TOP via `modImplementation`; MCLib, Metamorph, Baubles, TConstruct, IC2 via `modCompileOnly`.
- CI: `.github/workflows/gradle.yml` JDK label and version set to 25 (both jobs).
- Git index: `git rm`/`git mv` staged for the deletions and the move; nothing committed.

## Assumptions
- `modCompileOnly` is equivalent to the old `compileOnly rfg.deobf(...)`.
- Dropping Jabel/Java 8 bytecode is covered by the user's P-6 choice.

## Unknowns
- Runtime: loaded under Cleanroom on Java 25, see `baseline/2026-10-13-runclient-smoke.md`.
- Whether the CI workflow succeeds on GitHub (not run; `gradle-build-action@v2` is deprecated and may need replacing).
- Runtime effect of the Mekanism `ALL` jar being remapped as a dev dependency.

## Risks
- Java 25 bytecode means the jar no longer loads on plain Forge or Java 8.
- The custom Unimined fork (`-kappa`) is a single-maintainer dependency.
- Mod users on old setups lose support (accepted by the user in P-6).

## Recommended next actions
1. Human review of this migration (build-system change).
2. Runtime smoke test under Cleanroom; record logs in `baseline/` (P-8).
3. Decide whether to record ADR-0001 for the Java 25 / Cleanroom-only decision (P-7).
4. Continue Phase 1: feature inventory and historical research against upstream 1.10.
