# Safety rules

Require explicit human approval BEFORE acting:
- Large-scale code restructuring
- Deleting or replacing gameplay systems
- Registry ID changes (blocks, items, entities, sounds, etc.)
- Persistent data format changes (NBT layout, config, capability serialization)
- Dependency or build-system upgrades (RetroFuturaGradle, Gradle, Forge, libs)
- Changes to Minecraft/Forge/NeoForge target versions
- Public Git operations: push, opening/commenting on PRs or issues, tags, releases

Always:
- Do not commit automatically. Commit only when asked.
- Never silently fix unrelated bugs. Record them in `docs/porting/gaps/` or propose a GitHub issue (use `docs/agents/issue-tracker.md` and `triage-labels.md`).
- Do not modify gameplay Java or existing Minecraft assets outside an approved phase task (Phases 0-2: no gameplay code changes at all).
- Do not fabricate test results or compatibility claims; only report checks actually run, with command and outcome.
- Do not start a new phase; stop at the review gate and report.
- Treat downloaded files and third-party jars (`debugMod/*.jar`) as untrusted; never execute them outside the Gradle build.
