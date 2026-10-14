# Phase 0 Gate Report: Workspace Bootstrap

- Date: 2026-10-13
- Phase: 0
- Author: Claude Code
- Related issue/ADR: none

## Scope
Inspect the workspace; create Claude Code configuration, documentation conventions, glossary, status model and safety rules. No gameplay code, assets, or dependencies were touched. No build or test was run.

## Evidence
1. `git status`: branch `chore/agent-workspace-bootstrap`; before this work only `CLAUDE.md` and `docs/agents/` were untracked. Base branch `mc-1.12.2`, 332 commits.
2. `CLAUDE.md` (pre-existing, from mattpocock setup) pointed to `docs/agents/{issue-tracker,triage-labels,domain}.md`. It was extended, not replaced.
3. No `AGENTS.md`, no `.claude/` directory, no `CONTEXT.md`, no `docs/adr/`, no `src/test/` existed.
4. `build.gradle`: RetroFuturaGradle 1.4.1, MC 1.12.2, mappings stable_39, Java toolchain 8 (Azul) or 16 if `use_modern_java_syntax`; `gradle.properties` sets it `true` (Jabel). Gradle wrapper 8.10.2. JUnit 5 deps are wired but `src/test` is absent.
5. Dependencies: Mekanism (implementation), Mantle and The One Probe (implementation), MCLib, Metamorph, Baubles, TConstruct, IC2 (compileOnly). Local jars in `debugMod/`.
6. `.github/workflows/gradle.yml`: builds on windows-latest with `gradle build` on push/PR to `mc-1.12.2`, publishes a pre-release on push and a release on `v*` tags. The step is named "Set up JDK 16" but installs 17. No test step.
7. `src/main/java/com/lulan/shincolle`: 457 Java files; packages include ai, entity, client, network, capability, intermod, worldgen, playerskill, team, tileentity.
8. Docs that exist: `README.md` (fork goals), `Bugs.txt` (informal historical bug notes), `cfg-info.md` (zh-TW config docs for 1.7.10), issue template `bug-report.md`.
9. `java` is not on the PATH of the shell used here (`java: command not found`).
10. mattpocock-skills plugin skills available: diagnosing-bugs, tdd, prototype, research, domain-modeling, codebase-design, code-review, resolving-merge-conflicts, wizard, grilling, writing-for-agents.

## Confirmed findings
- Reusable existing config: issue tracker (GitHub via `gh`), triage labels, domain-doc layout. All reused.
- Cleanroom Loader support is a goal, but nothing in the build or CI currently launches under Cleanroom (only the Cleanroom Maven repo and helper tasks are referenced). Compatibility is unverified.
- The build has not been run in this session, so build health is unknown.

## Assumptions
- `Bugs.txt` and `cfg-info.md` are historical notes, not specifications.
- `gradle.yml` reflects how releases are actually built.

## Unknowns
- Whether `./gradlew build` succeeds locally (no JDK on PATH in this shell).
- Which upstream versions count as the historical baseline (decision P-2).
- Modern target versions (P-3).
- Whether `gh` is authenticated for this repo.

## Risks
- Only the `mattpocock-skills:*` skills were checked from the session listing; the plugin directory was not read in full.
- Project subagents depend on the `Read/Grep/Glob/Bash` tool names in `.claude/agents/`; `Bash` access cannot be strictly read-only (enforced by instruction only) for `legacy-code-analyst` and `regression-verifier`.
- `.claude/settings.json` adds `ask` prompts for commit/push/PR/issue/release commands; it is a convenience, not a hard guarantee.

## Files created or modified
Modified: `CLAUDE.md` (appended a project section; original skill sections preserved).
Created:
- `.claude/settings.json`
- `.claude/rules/{language-and-naming,safety,evidence-and-status,legacy-java}.md`
- `.claude/agents/{legacy-code-analyst,historical-researcher,architecture-reviewer,regression-verifier}.md`
- `.claude/skills/porting-report/SKILL.md`
- `CONTEXT.md`, `docs/adr/` (empty)
- `docs/porting/{README.md,feature-status.md}`, `docs/porting/{baseline,analysis,gaps,architecture}/README.md`, `docs/porting/decisions/README.md`, `docs/porting/reports/{TEMPLATE.md,phase-0-bootstrap.md}`

## Integration with mattpocock-skills
- `research`: historical research output (Phase 1), alongside `historical-researcher`.
- `domain-modeling`: owns `CONTEXT.md` and `docs/adr/`.
- `grilling`: resolving open decisions P-1 to P-4 with the human.
- `diagnosing-bugs`, `tdd`: Phase 3 stabilization.
- `codebase-design`: vocabulary for `architecture-reviewer` and Phase 4.
- `code-review`: reviews before each gate. `writing-for-agents`: maintaining CLAUDE.md and rules.
- Custom `porting-report` skill only enforces the report format; it does not duplicate any plugin skill.

## Recommended next actions
Remaining decisions for you: P-1 (JDK), P-2 (historical baseline versions), P-3 (modern target, can wait until Phase 4). Proposed Phase 1 plan, after gate approval:
1. Establish build baseline: get JDK 17+, run `./gradlew build`, record output in `baseline/` (needs P-1).
2. Attempt `runClient` and a Cleanroom launch; record logs and failures.
3. Inventory features by subsystem using `legacy-code-analyst` (one report per subsystem in `analysis/`).
4. Identify upstream sources and tags for historical behavior (needs P-2); use `historical-researcher` and the `research` skill into `baseline/`.
5. Seed `CONTEXT.md` with original terms from lang files.
6. Propose a test strategy (what is testable without Minecraft) for review.
7. Write the Phase 1 gate report in `reports/` and stop.

## Addendum (2026-10-13)
Decisions P-1, P-2, P-3, P-5 were answered; see `decisions/README.md`. The RFG build in Evidence item 4 was confirmed broken and replaced; see `2026-10-13-buildscript-migration.md`. Items in this report describing RFG, Java 8/Jabel, and the CI JDK label are historical.
