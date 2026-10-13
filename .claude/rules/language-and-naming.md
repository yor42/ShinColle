# Language and naming

- Write all new docs, prompts, agent/skill definitions, ADRs, reports, issue titles and code comments in English.
- Preserve existing localization assets (`assets/shincolle/lang/*`), registry IDs, resource paths, and legacy gameplay terms exactly. Do not translate, rename, or "normalize" them without explicit approval.
- Texture/resource paths are lowercase on purpose (case-sensitive filesystems); do not change.
- When a JP/CN/KR term has no precise English equivalent, add an entry to `CONTEXT.md` with the original form and the agreed English term. If no English term is agreed yet, mark it `PROPOSED` and ask.
- In prose, use glossary terms from `CONTEXT.md`; do not drift to synonyms.
- Mod ID is `shincolle`; root package `com.lulan.shincolle`.
