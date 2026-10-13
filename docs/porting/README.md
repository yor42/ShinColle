# ShinColle Porting Workspace

**Current phase: 3 (Legacy Stabilization), started 2026-10-13 after the user approved the Phase 2 gate. Plan and exit criteria: decision P-13 (`decisions/README.md`); backlog `gaps/2026-10-13-gap-backlog.md`; issues #1-#8.**
Update this line only when the human approves a gate.

Goal: maintain a functional 1.12.2 (Forge, Cleanroom-compatible) implementation while building a NeoForge implementation that shares Minecraft-independent logic and content definitions. The existing 1.12.2 code is an unofficial, incomplete port and is not a behavioral specification.

## Directory map
| Path | Purpose |
|---|---|
| `baseline/` | Phase 1: build/run baseline, feature inventory, historical behavior records |
| `analysis/` | Subsystem analyses of current code |
| `gaps/` | Phase 2: gap records with feature status |
| `architecture/` | Seams, shared-module candidates, design notes |
| `decisions/` | Pending decisions log; accepted decisions become ADRs in `docs/adr/` |
| `reports/` | Gate reports and verification reports; `TEMPLATE.md` is the common format |
| `feature-status.md` | Feature status model |
| `../../CONTEXT.md` | Glossary (mattpocock `domain-modeling` convention) |
| `../adr/` | Architecture Decision Records |
| `../agents/` | Issue tracker, triage labels, domain-doc config (mattpocock setup) |

## Phases
Phases never advance automatically. At each gate the agent writes a gate report in `reports/` and stops; the human approves, amends, or rejects.

### 0. Workspace Bootstrap
- Deliverables: CLAUDE.md, `.claude/` rules/agents/skill, `docs/porting/` structure, glossary, status model, safety rules, Phase 0 report.
- Acceptance: files exist and are consistent; no gameplay code, assets, or dependencies changed; remaining decisions listed.
- Gate: human reviews `reports/phase-0-bootstrap.md`.

### 1. Baseline and Historical Research
- Deliverables: verified build/run baseline (JDK, Gradle, client launch under Cleanroom, with logs); feature inventory by subsystem; historical behavior records from upstream ShinColle 1.10 (the version the port is based on); test strategy proposal.
- Acceptance: every baseline claim has a recorded command and output; each historical fact cites a primary source and version; unknowns are listed, not guessed.
- Gate: human reviews baseline report and inventory.

### 2. Gap Analysis
- Deliverables: per-feature records with status (see `feature-status.md`), evidence, confidence; prioritized gap list; GitHub issues proposed (not created without approval).
- Acceptance: no feature is CONFIRMED_WORKING without runtime or test evidence; historical, current, and desired behavior are separate; UNKNOWN_INTENT items are routed to the human.
- Gate: human approves prioritization and which gaps are PLANNED_DEVIATION.

### 3. Legacy Stabilization
- Deliverables: per approved issue, a characterization test or documented manual check first, then a small fix (red-green-refactor, `mattpocock-skills:tdd`); regression-verifier report.
- Acceptance: each change is small, independently verifiable, in scope; no registry/NBT/config format change without approval; build result recorded from an actual run.
- Gate: human reviews per batch before merge; stabilization exit criteria are defined at the Phase 2 gate.

### 4. Multi-Version Bootstrap
- Deliverables: ADR(s) for module layout (shared core vs. legacy vs. NeoForge); NeoForge/MC target version decision; build skeleton compiling an empty shared module on both tracks; data/ID compatibility policy.
- Acceptance: ADRs approved; both tracks build in CI; legacy track unaffected.
- Gate: human approves architecture and target versions before any extraction.

### 5. Incremental Dual-Track Development
- Deliverables: feature-by-feature slices: shared logic extracted with tests, legacy adapter, NeoForge adapter.
- Acceptance: each slice has tests on shared logic, no legacy regression, status records updated.
- Gate: per-slice human review; periodic re-planning.

## Report format
All reports use `reports/TEMPLATE.md`: Scope, Evidence, Confirmed findings, Assumptions, Unknowns, Risks, Recommended next actions. Name files `YYYY-MM-DD-topic.md`.
