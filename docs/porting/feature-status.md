# Feature Status Model

Each feature (a gameplay capability such as "ship leveling" or "Training Book consumption") has one status for the CURRENT 1.12.2 implementation.

| Status | Meaning | Minimum evidence |
|---|---|---|
| `CONFIRMED_WORKING` | Behaves as intended, verified | Automated test, or recorded in-game/runtime observation (steps, version, log). Source existing is NOT evidence. |
| `CONFIRMED_BUG` | Behaves incorrectly relative to intended behavior | Reproduction steps or failing test, plus a source for the intended behavior |
| `INCOMPLETE_PORT` | Present in earlier versions but missing, stubbed (NYI), or partial here | Historical source citation AND current-code citation showing the gap |
| `UNKNOWN_INTENT` | Cannot determine what behavior is correct | What was checked and why it is inconclusive; routed to the human |
| `PLANNED_DEVIATION` | Intentionally differs from historical behavior | Human-approved decision (ADR or issue link) |

Rules:
- Every status record carries **evidence** and a **confidence** (HIGH / MEDIUM / LOW).
- Track three columns separately: **Historical** (earlier ShinColle, with version/source), **Current** (this repo, with commit), **Desired** (future, human-approved). Never merge them.
- An unexamined feature has no status. It is never assumed CONFIRMED_WORKING.
- A status change needs new evidence recorded in the same entry (keep history; append dated lines).
- Only PLANNED_DEVIATION requires human approval to set; the agent may propose the others.
- Records live in gap/analysis docs using the table in `reports/TEMPLATE.md`.
