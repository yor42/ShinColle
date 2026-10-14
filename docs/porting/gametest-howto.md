# Headless gametest how-to

Dev-only harness in `src/gametest/` (second mod `shincolle_gametest`). History and evidence: `docs/porting/reports/2026-10-13-gametest-harness.md`. Every command and path below was read from `build.gradle:151-211`, `GameTestRunner`, `GameTestServerHooks` and `ShinColleGameTest`; none was run while writing this page.

## Caveats
- The client run needs a desktop and a visible window (LWJGL 2); it is not headless or CI-ready.
- It creates a new superflat creative world `gametest-<timestamp>` under `run/client/saves` and writes into `run/client`.
- Use one Gradle process and one client at a time. Do not run another Gradle task against the checkout meanwhile.
- The harness drives packets through the client controller, not mouse or key input.

## 1. Client run (single player)
```
./gradlew.bat runClient -Pgametest
```
- `-Pgametest` puts the `gametest` output on the runtime path and sets `-Dshincolle.gametest=true` on `runClient`.
- Results: `run/client/gametest/results.txt` (lines `PASS`, `FAIL`, `INFO`, final `SUMMARY passed=N failed=M`); log lines are prefixed `[GAMETEST]`; screenshots are saved as `gametest-<name>.png` in the client `screenshots` folder.
- The client quits when finished unless `-Dshincolle.gametest.keepOpen=true` is set.

## 2. Dedicated server plus client
1. Generate the launch files: `./gradlew.bat gametestServerArgs -Pgametest` (the task exists only with `-Pgametest`). It writes `build/gametest-server.args` (JVM args, classpath, main class, includes `-Dshincolle.gametest.server=true`) and `build/gametest-server.env` (environment variables for the launcher), and forces spawn keys in `run/server-gametest/server.properties`: `spawn-animals=true`, `spawn-npcs=true`, `spawn-monsters=false`. Ships are animals; with spawn-animals off vanilla removes them.
2. Start the server with plain `java @build/gametest-server.args` from `run/server-gametest` (the `build.gradle` comment: separate working directory, no second Gradle process), with the variables from `gametest-server.env` set. This launch recipe is inferred from that comment and was not run here. RCON must be enabled in `server.properties` by hand.
3. Start the client: `./gradlew.bat runClient -Pgametest -Pgametest_connect=host:port -Pgametest_rcon=host:port -Pgametest_rconpass=<password>`. `gametest_rcon` defaults to `127.0.0.1:25631`, the password to empty. Each `gametest_<key>` property becomes `-Dshincolle.gametest.<key>`.
4. Client results go to `run/client/gametest/dedicated-results.txt` (used instead of `results.txt` when `connect` is set). The server probe command is `/gtprobe state|egg|fakeowner|reset|stop` (`GameTestServerHooks`).
5. Start each dedicated run from a fresh `run/server-gametest` world (`world-gametest`); a stale world can break the single-ship precondition (harness report, issue #9 note).

### Spawn-off case (issue #10)
Pass `-Pgametest_spawnoff=true` to BOTH `gametestServerArgs` (writes `spawn-animals=false`, `spawn-npcs=false`) and `runClient` (`-Dshincolle.gametest.spawnoff`). It exercises the spawn-sweep mixin.

## 3. Headless ship-load self-test (no client, no RCON)
Add `-Dshincolle.gametest.shipload=true` to the server JVM command (before `@build/gametest-server.args`; the args file already sets `shincolle.gametest.server=true`, which registers the hooks). After tick 40 the server summons three Destroyer I ships (no, partial and `Minor`-only `ShipExtProps`) at world spawn, waits 300 ticks, checks them, writes `gametest-shipload-results.txt` into the server working directory (`run/server-gametest/gametest-shipload-results.txt`) and shuts the server down. Lines are `PASS`/`FAIL name: ...` and `SUMMARY cases=3 failed=N`.

## Result files
| File | Written by | When |
|---|---|---|
| `run/client/gametest/results.txt` | client runner | single-player run |
| `run/client/gametest/dedicated-results.txt` | client runner | `gametest_connect` set |
| `run/server-gametest/gametest-shipload-results.txt` | server hooks | `shipload=true` |

## Known gaps
See "Coverage lost by the harness fix" (C1, C2) in the harness report. Passing runs show stability in one setup, not proof.
