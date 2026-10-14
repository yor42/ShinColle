---
name: porting-report
description: Create a ShinColle porting report or feature-status entry in the standard format. Use when recording baseline, analysis, gap, or verification findings under docs/porting/.
disable-model-invocation: true
---

Create the report from `docs/porting/reports/TEMPLATE.md`.

1. Pick the directory by phase/kind: `baseline/`, `analysis/`, `gaps/`, `architecture/`, `reports/` (under `docs/porting/`). File name: `YYYY-MM-DD-short-topic.md`.
2. Fill every section: Scope, Evidence, Confirmed findings, Assumptions, Unknowns, Risks, Recommended next actions. Write "None" rather than omitting a section.
3. For each feature, add a row using the status model in `docs/porting/feature-status.md`: status, confidence, historical / current / desired behavior as separate columns, evidence links. Do not mark CONFIRMED_WORKING from source existence alone.
4. Use glossary terms from `CONTEXT.md`; add new JP/CN/KR terms there.
5. Do not commit. Tell the user the path and stop for review.

For deep sourced research, use `mattpocock-skills:research`; for decisions, use `mattpocock-skills:domain-modeling` (ADRs in `docs/adr/`). This skill only enforces the report format.
