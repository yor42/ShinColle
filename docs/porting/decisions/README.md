# Decisions

Accepted architectural decisions are ADRs in `docs/adr/` (create them via `mattpocock-skills:domain-modeling`). This directory holds the **decisions log** only.

Decisions requiring human approval are listed in `.claude/rules/safety.md`.

## Resolved (2026-10-13, by user; P-7 onward from the Phase 2 grilling session)
| ID | Decision |
|---|---|
| P-1 | The original RFG buildscript no longer resolves (confirmed). Migrate to the CleanroomModTemplate-mixin buildscript (Unimined). Done. |
| P-2 | Historical baseline is upstream ShinColle 1.10, which the 1.12.2 port is based on. |
| P-5 | CI JDK label and version fixed together with the buildscript migration. Done. |
| P-6 | Java 25, Cleanroom only. Recorded as `docs/adr/0001-java-25-cleanroom-only.md`. |
| P-7 | Combat: fix CMB-01 (triple hit reachable: miss > crit > double > triple) and CMB-02 (day table by day), each after a characterization test. Decided 2026-10-13. |
| P-8 | Metamorph ship-morph: supported on the legacy track, out of scope for the Modern track; morph logic never enters the shared core. `docs/adr/0003-ship-morph-legacy-only.md`. |
| P-9 | Multiplayer: public/dedicated servers in scope; server-authoritative validation of every client request; NET-02 joins the first stabilization batch. `docs/adr/0004-server-authoritative-packets.md`. |
| P-10 | PLANNED_DEVIATION approved: ATTR-05, CMB-04, CMB-07, MOD-04/05/07/08/09, RND-05 (pending visual check), RND-06 (link hints restored later, low priority), AI-08 (as-is for now; **must be fixed later**: persistence and matching), INV-02 (add migration of the old 2 values). ATTR-04 rejected: treat as a bug, low priority. |
| P-11 | Save compatibility: worlds from this fork's 1.12.2 releases must load with no loss; 1.10.2 worlds unsupported. `docs/adr/0002-save-compatibility-target.md`. |
| P-12 | Sounds: restore the `ship-idle-60`/`ship-hit-60`/`ship-hit-62` entries, fix `hurt54013` → `hurt5403`, remove the `feed3301/3302` references. |
| P-14 | AI-02: a stationary guard fires at the same per-tick cadence as a moving guard (fix together with AI-01). |
| P-15 | NET-09 keep polling on legacy (Modern designs sync fresh); ENT-07 revive at the cached position; ENT-11 ships and portals unsupported (documented); ENT-04 boss cooldown follows `Check_Ring` (bug); INV-08 death penalty on the Ship Spawn Egg is PLANNED_DEVIATION; INV-10 deferred to Phase 4; MOD-12 keep dev runtime mods, add an absent-mod launch check to Phase 3. |
| P-13 | Phase 3 first batch (after task 0: world-entry smoke run, `src/test` bootstrap, absent-mod launch check): ATTR-03 (#1) → NET-02 (#2) → AI-01+AI-02 (#3) → NET-04/ATTR-01/02 (#4). Exit criteria: P0 fixed (ATTR-03, AI-01, AI-02, NET-04, ENT-08, AI-03, INV-03, ENT-02); NET-01/02/03; INV-01, INV-02 migration, P-12 sounds, CMB-01, CMB-02, ENT-04; each fix has a test or recorded manual check; build passes from a real run; a fork-release save fixture loads; absent-mod launch matrix passes; no registry ID or NBT key renamed or removed. Everything else may land in Phase 5. |
| P-16 | Metamorph, MCLib and Baubles become runtime dependencies behind a Gradle property, default off (absent-mod runs stay the default). Build change approved; implement in Phase 3 task 0. |
| P-17 | AI-08 fix (Phase 5): persist the per-player opt-out as a new additive key in the player capability, match whole words, ignore cancelled chat; default stays on. |
| P-18 | GitHub issues #1-#8 created 2026-10-13 from the Phase 2 backlog (labels `bug`, `ready-for-agent`). |
| P-19 | Glossary terms accepted (Admiral, Grudge, Training Book, Modernization Toolkit, Repair Goddess, Drum, Small/Large Shipyard, Crane, Ship Spawn Egg, Guard mode); `CONTEXT.md` converted to the standard glossary format. |

## Pending
| ID | Question | Needed by | Options | Status |
|---|---|---|---|---|
| P-3 | Modern target: NeoForge and Minecraft version | Phase 4 | user to specify | deferred |
| P-4 | Module layout for the shared core | Phase 4 | ADR | open |
