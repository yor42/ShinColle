# Evidence and feature status

- Never classify a feature as working because its source exists. `CONFIRMED_WORKING` needs runtime or test evidence.
- Track three things separately per feature: historical behavior (earlier ShinColle, e.g. 1.7.10/1.10), current 1.12.2 behavior, desired future behavior.
- Every status needs: evidence (file:line, test output, log, in-game observation, upstream source URL/commit) and a confidence level (HIGH/MEDIUM/LOW).
- Separate Confirmed findings from Assumptions and Unknowns in every report. Use the template in `docs/porting/reports/TEMPLATE.md`.
- Statuses and definitions: `docs/porting/feature-status.md`.
- Upstream history lives in forks: PinkaLulan/ShinColle (original), Cappycot, misaka10843, then yor42. Prefer primary sources (git history, source, official docs) over memory.
