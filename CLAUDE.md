## Agent skills

### Issue tracker

Issues are tracked in GitHub Issues on yor42/ShinColle via the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

Uses the five default triage labels (needs-triage, needs-info, ready-for-agent, ready-for-human, wontfix). See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `CONTEXT.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.

## Project: ShinColle porting workspace

Unofficial, incomplete 1.12.2 port of ShinColle (shipgirl entities, combat, AI, equipment, progression). Branch `mc-1.12.2`. Built with Unimined (Cleanroom fork) following the CleanroomModTemplate-mixin buildscript: Gradle 9.8.0, Java 25 toolchain/bytecode, Cleanroom loader 0.6.13-alpha. The mod targets Cleanroom only (decision 2026-10-13); plain Forge/Java 8 is not supported. Historical baseline is upstream ShinColle 1.10. Long-term: keep the legacy 1.12.2 build maintained AND build a NeoForge implementation sharing Minecraft-independent logic.

**The current 1.12.2 code is NOT a behavioral specification.** Its existence in source does not mean a feature works or matches earlier ShinColle versions.

### Read first
- `docs/porting/README.md`: phases, gates, current phase, directory map
- `docs/porting/feature-status.md`: status model (CONFIRMED_WORKING etc.) and evidence rules
- `CONTEXT.md`: glossary (use these terms; record original JP/CN/KR terms there)
- `.claude/rules/`: language, safety, and evidence rules (loaded automatically)

### Workflow
Use the `mattpocock-skills` plugin as the primary toolkit (`research`, `domain-modeling`, `grilling`, `diagnosing-bugs`, `tdd`, `codebase-design`, `code-review`, `writing-for-agents`). Do not add a competing PM framework. Project-specific pieces: subagents in `.claude/agents/` (read-only specialists) and the `porting-report` skill.

Phases (never auto-transition; each ends at a human review gate): 0 Workspace Bootstrap, 1 Baseline and Historical Research, 2 Gap Analysis, 3 Legacy Stabilization, 4 Multi-Version Bootstrap, 5 Incremental Dual-Track Development. Current phase is recorded in `docs/porting/README.md`.

### Hard rules (details in `.claude/rules/safety.md`)
- No commits, pushes, or PRs unless the user asks. Ask before any public Git operation.
- Ask before: large refactors, removing/replacing gameplay systems, registry ID changes, persistent data format changes, dependency/build upgrades, MC/Forge/NeoForge target changes.
- Never silently fix unrelated bugs; log them as findings instead.
- Never claim a test/build passed without running it. Report actual output.
- English for all new docs, comments, prompts, issue titles. Never translate or rename legacy content, registry IDs, resource paths, or lang files without approval.

### Commands
- Build: `./gradlew build` with `JAVA_HOME` set to a JDK 25 (e.g. `~/.jdks/temurin-25.0.4`); Unimined requires Gradle itself to run on JVM 25. Verified 2026-10-13: BUILD SUCCESSFUL (see `docs/porting/reports/2026-10-13-buildscript-migration.md`).
- Mods in `debugMod/` resolve through a flatDir repo (`gradle/scripts/dependencies.gradle`); use `mod*` configurations for any mod dependency.
- Tests: JUnit 5 is enabled but `src/test` does not exist yet. Runtime: `runClient` loads the mod under Cleanroom (see `docs/porting/baseline/2026-10-13-runclient-smoke.md`); in-world behavior is not yet verified.
